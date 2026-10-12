"""주문 대량 적재 도구 공통 모듈 (LLD-0006).

DB 접근은 드라이버 없이 `docker exec mysql mysql ...`을 subprocess로 호출한다.
SQL은 표준 입력으로 넘겨 셸 인용·경로 변환(Git Bash) 문제를 피한다.
"""

from __future__ import annotations

import csv
import json
import math
import subprocess
import sys
import time
from dataclasses import dataclass
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent
FIXTURES = ROOT / "fixtures"
OUT = ROOT / "out"

MYSQL_CONTAINER = "mysql"
DB = "festival2"
DB_USER = "root"
DB_PASSWORD = "1234"
IN_CONTAINER_DIR = "/var/lib/mysql-files/order-seed"

BASE_SEED = 20261012
SEOKYEONG_ID = 1
HOT_UNIVERSITY_IDS = (1, 46, 103, 172, 343)
GROUPS = ("HOT", "WARM", "ETC")
GROUP_WEIGHTS = (50, 35, 15)
POPULAR_SHARE = (70, 30)  # 인기 3곳 합 / 나머지
REP_SHARE = 0.65  # 대표 메뉴 판매 수량 비중
STATUSES = ("WAITING", "COOKING", "COMPLETED", "CANCELED")
STATUS_WEIGHTS = (5, 10, 75, 10)
MENU_COUNTS = (1, 2, 3, 4)
MENU_COUNT_WEIGHTS = (10, 25, 50, 15)
MIN_ELIGIBLE_MENUS = 3  # 메뉴 2개 이하 부스는 신규 주문 대상에서 제외 (LLD-0006 Q3)


class ToolError(Exception):
    """도구 실행을 멈춰야 하는 오류. 메시지를 출력하고 종료 코드 1로 끝낸다."""


def fail(message: str) -> None:
    print(f"[ERROR] {message}", file=sys.stderr)
    sys.exit(1)


def _run_mysql(sql: str, database: bool = True) -> str:
    cmd = ["docker", "exec", "-i", MYSQL_CONTAINER, "mysql", f"-u{DB_USER}", f"-p{DB_PASSWORD}",
           "--default-character-set=utf8mb4", "-B", "-N"]
    if database:
        cmd.append(DB)
    try:
        proc = subprocess.run(cmd, input=sql.encode("utf-8"), capture_output=True)
    except FileNotFoundError as e:
        raise ToolError("docker 명령을 찾을 수 없습니다. Docker Desktop 실행 여부를 확인하세요.") from e
    stderr = "\n".join(line for line in proc.stderr.decode("utf-8", "replace").splitlines()
                       if "Using a password on the command line" not in line)
    if proc.returncode != 0:
        raise ToolError(f"MySQL 실행 실패 (exit {proc.returncode}): {stderr.strip()}")
    return proc.stdout.decode("utf-8")


def query(sql: str) -> list[list[str]]:
    """SELECT 결과를 탭 분리 행 목록으로 돌려준다."""
    out = _run_mysql(sql)
    return [line.split("\t") for line in out.splitlines() if line != ""]


def query_int(sql: str) -> int:
    rows = query(sql)
    value = rows[0][0] if rows else "NULL"
    return 0 if value == "NULL" else int(value)


def execute(sql: str) -> float:
    """SQL을 실행하고 소요 초를 돌려준다."""
    start = time.perf_counter()
    _run_mysql(sql)
    return time.perf_counter() - start


def docker_cp(local_path: Path, container_path: str) -> None:
    proc = subprocess.run(["docker", "cp", str(local_path), f"{MYSQL_CONTAINER}:{container_path}"],
                          capture_output=True)
    if proc.returncode != 0:
        raise ToolError(f"docker cp 실패: {proc.stderr.decode('utf-8', 'replace').strip()}")


def docker_exec(*args: str) -> None:
    proc = subprocess.run(["docker", "exec", MYSQL_CONTAINER, *args], capture_output=True)
    if proc.returncode != 0:
        raise ToolError(f"docker exec 실패: {proc.stderr.decode('utf-8', 'replace').strip()}")


def largest_remainder(total: int, weights: list[float] | tuple[float, ...]) -> list[int]:
    """total을 weights 비율로 나눈다. 각 몫을 내림한 뒤 소수부가 큰 순서(동점은 앞 인덱스)로 1씩 더 준다."""
    if total < 0:
        raise ToolError(f"음수는 배분할 수 없습니다: {total}")
    weight_sum = sum(weights)
    if weight_sum <= 0:
        if total == 0:
            return [0] * len(weights)
        raise ToolError("가중치 합이 0인데 배분할 건수가 있습니다")
    raw = [total * w / weight_sum for w in weights]
    base = [math.floor(r) for r in raw]
    remain = total - sum(base)
    order = sorted(range(len(weights)), key=lambda i: (-(raw[i] - base[i]), i))
    for i in order[:remain]:
        base[i] += 1
    assert sum(base) == total
    return base


def water_fill(required: int, targets: list[int], current: list[int]) -> list[int]:
    """required건을 '목표 대비 현재가 가장 모자란 칸'부터 채운다 (물 채우기).

    목표를 이미 넘은 칸은 받지 않는다. 모든 칸이 목표를 채운 뒤에도 남으면 수준을 고르게 올린다.
    결과 합은 항상 required다.
    """
    n = len(targets)
    if required < 0:
        raise ToolError(f"물 채우기 대상 건수가 음수입니다: {required}")
    if n == 0:
        if required == 0:
            return []
        raise ToolError("배정할 칸이 없는데 건수가 남았습니다")
    if required == 0:
        return [0] * n
    deviation = [c - t for c, t in zip(current, targets)]  # 낮을수록 모자람

    def filled(level: int) -> int:
        return sum(max(0, level - d) for d in deviation)

    lo, hi = min(deviation), min(deviation) + required + 1
    while lo < hi:  # filled(level) >= required 인 최소 level
        mid = (lo + hi) // 2
        if filled(mid) >= required:
            hi = mid
        else:
            lo = mid + 1
    level = lo
    alloc = [max(0, level - 1 - d) for d in deviation]
    remain = required - sum(alloc)
    for i in range(n):
        if remain == 0:
            break
        if deviation[i] <= level - 1:
            alloc[i] += 1
            remain -= 1
    assert sum(alloc) == required and remain == 0
    return alloc


@dataclass
class Menu:
    menu_id: int
    price: int
    is_rep: bool


@dataclass
class Fixtures:
    groups: dict[int, str]  # university_id → HOT|WARM|ETC
    booth_university: dict[int, int]  # 모든 부스 → university_id
    menus: dict[int, list[Menu]]  # 신규 주문 대상 부스 → 주문 가능 메뉴
    popular: dict[int, list[int]]  # HOT university_id → 인기 부스 3개
    festival: dict[int, list[date]]  # university_id → 축제 3일
    baseline: dict


def load_fixtures() -> Fixtures:
    required = ["groups.csv", "booths.csv", "menus.csv", "popular_booths.csv", "festival_dates.csv",
                "baseline.json"]
    missing = [name for name in required if not (FIXTURES / name).exists()]
    if missing:
        raise ToolError(f"fixture가 없습니다: {missing} — prepare.py를 먼저 실행하세요")

    def rows(name: str) -> list[dict[str, str]]:
        with open(FIXTURES / name, encoding="utf-8", newline="") as f:
            return list(csv.DictReader(f))

    groups = {int(r["university_id"]): r["group"] for r in rows("groups.csv")}
    booth_university = {int(r["booth_id"]): int(r["university_id"]) for r in rows("booths.csv")}
    menus: dict[int, list[Menu]] = {}
    for r in rows("menus.csv"):
        menus.setdefault(int(r["booth_id"]), []).append(
            Menu(int(r["menu_id"]), int(r["price"]), r["is_rep"] == "1"))
    popular: dict[int, list[int]] = {}
    for r in rows("popular_booths.csv"):
        popular.setdefault(int(r["university_id"]), []).append(int(r["booth_id"]))
    festival = {int(r["university_id"]): [date.fromisoformat(r[k]) for k in ("day1", "day2", "day3")]
                for r in rows("festival_dates.csv")}
    with open(FIXTURES / "baseline.json", encoding="utf-8") as f:
        baseline = json.load(f)
    return Fixtures(groups, booth_university, menus, popular, festival, baseline)


def orders_by_booth() -> dict[int, int]:
    """부스별 주문 수 (주문 1건 = 부스 1곳 전제)."""
    rows = query("""
        SELECT bm.booth_id, COUNT(DISTINCT oi.order_id)
        FROM order_items oi JOIN booth_menu bm ON bm.id = oi.booth_menu_id
        GROUP BY bm.booth_id""")
    return {int(b): int(c) for b, c in rows}


def qty_by_menu() -> dict[int, int]:
    rows = query("SELECT booth_menu_id, SUM(quantity) FROM order_items GROUP BY booth_menu_id")
    return {int(m): int(q) for m, q in rows}


def qty_by_booth() -> dict[int, int]:
    rows = query("""
        SELECT bm.booth_id, SUM(oi.quantity)
        FROM order_items oi JOIN booth_menu bm ON bm.id = oi.booth_menu_id
        GROUP BY bm.booth_id""")
    return {int(b): int(q) for b, q in rows}


def menu_count_distribution() -> dict[int, int]:
    """주문당 메뉴(항목) 수별 주문 수. 항목 없는 주문은 0."""
    rows = query("""
        SELECT k, COUNT(*) FROM (
          SELECT o.id, COUNT(oi.id) k FROM orders o LEFT JOIN order_items oi ON oi.order_id = o.id GROUP BY o.id
        ) t GROUP BY k""")
    return {int(k): int(c) for k, c in rows}


def status_distribution() -> dict[str, int]:
    rows = query("SELECT order_status, COUNT(*) FROM orders GROUP BY order_status")
    return {s: int(c) for s, c in rows}


def max_ids() -> dict[str, int]:
    return {
        "orders": query_int("SELECT MAX(id) FROM orders"),
        "order_items": query_int("SELECT MAX(id) FROM order_items"),
        "order_item_units": query_int("SELECT MAX(id) FROM order_item_units"),
    }
