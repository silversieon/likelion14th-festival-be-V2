"""[0] 사전 준비 (LLD-0006 5장) — 1회 실행.

1) 고정 목록(fixture)을 만든다: 학교 그룹, 부스→학교, 주문 가능 메뉴(가격·대표 여부), Hot 인기 부스, 학교별 축제 3일, 기준값
2) 모든 부스를 OPEN + order_enabled = 1로 바꾼다

사용: python prepare.py [--seed 20261012] [--force] [--skip-booth-update]
"""

from __future__ import annotations

import argparse
import csv
import json
import random
from datetime import datetime

from common import (BASE_SEED, FIXTURES, HOT_UNIVERSITY_IDS, MIN_ELIGIBLE_MENUS, SEOKYEONG_ID, ToolError,
                    execute, fail, max_ids, orders_by_booth, qty_by_menu, query, query_int)

# 연속 평일 3일 창 — 2026년 5월 2주~6월 1주 중 공휴일(5/25 대체공휴일, 6/3 지방선거일)을 포함하지 않는 것 (LLD-0006 6.3, Q2)
FESTIVAL_WINDOWS = [
    ("2026-05-11", "2026-05-12", "2026-05-13"),
    ("2026-05-12", "2026-05-13", "2026-05-14"),
    ("2026-05-13", "2026-05-14", "2026-05-15"),
    ("2026-05-18", "2026-05-19", "2026-05-20"),
    ("2026-05-19", "2026-05-20", "2026-05-21"),
    ("2026-05-20", "2026-05-21", "2026-05-22"),
    ("2026-05-26", "2026-05-27", "2026-05-28"),
    ("2026-05-27", "2026-05-28", "2026-05-29"),
]
SEOKYEONG_WINDOW = ("2026-05-13", "2026-05-14", "2026-05-15")


def build_groups() -> dict[int, str]:
    groups = {}
    for uid, region in query("SELECT id, region FROM universities ORDER BY id"):
        uid = int(uid)
        if uid in HOT_UNIVERSITY_IDS:
            groups[uid] = "HOT"
        elif region == "SEOUL":
            groups[uid] = "WARM"
        else:
            groups[uid] = "ETC"
    return groups


def build_booth_university() -> dict[int, int]:
    rows = query("SELECT b.id, d.university_id FROM booth b JOIN departments d ON d.id = b.department_id ORDER BY b.id")
    return {int(b): int(u) for b, u in rows}


def build_menus(booth_university: dict[int, int]) -> dict[int, list[tuple[int, int]]]:
    """신규 주문 대상 부스 → [(menu_id, price)]. 품절·0원 제외, 메뉴 3개 이상인 부스만."""
    rows = query("SELECT booth_id, id, price FROM booth_menu WHERE is_sold_out = 0 AND price > 0 ORDER BY booth_id, id")
    menus: dict[int, list[tuple[int, int]]] = {}
    for booth_id, menu_id, price in rows:
        menus.setdefault(int(booth_id), []).append((int(menu_id), int(price)))
    return {b: m for b, m in menus.items() if len(m) >= MIN_ELIGIBLE_MENUS and b in booth_university}


def build_rep_menus(menus: dict[int, list[tuple[int, int]]], booth_university: dict[int, int],
                    menu_qty: dict[int, int]) -> dict[int, tuple[int, int]]:
    """대표 메뉴 2개. 서경대는 기존 판매 수량 상위 2개(동점은 id 작은 쪽), 그 외는 id가 가장 작은 2개."""
    rep = {}
    for booth_id, items in menus.items():
        ids = [m for m, _ in items]
        if booth_university[booth_id] == SEOKYEONG_ID:
            ranked = sorted(ids, key=lambda m: (-menu_qty.get(m, 0), m))
        else:
            ranked = sorted(ids)
        rep[booth_id] = (ranked[0], ranked[1])
    return rep


def build_popular_booths(menus: dict[int, list[tuple[int, int]]], booth_university: dict[int, int],
                         booth_orders: dict[int, int]) -> dict[int, list[int]]:
    """Hot 학교별 인기 부스 3곳. 서경대는 기존 주문 상위 3곳, 그 외는 주문 가능 부스 중 id가 가장 작은 3곳."""
    popular = {}
    for uid in HOT_UNIVERSITY_IDS:
        eligible = sorted(b for b in menus if booth_university[b] == uid)
        if uid == SEOKYEONG_ID:
            eligible.sort(key=lambda b: (-booth_orders.get(b, 0), b))
        if len(eligible) < 4:
            raise ToolError(f"Hot 학교 {uid}의 주문 가능 부스가 4곳 미만입니다: {eligible}")
        popular[uid] = eligible[:3]
    return popular


def build_festival_dates(groups: dict[int, str], rng: random.Random) -> dict[int, tuple[str, str, str]]:
    others = sorted(u for u in groups if u != SEOKYEONG_ID)
    rng.shuffle(others)
    dates = {SEOKYEONG_ID: SEOKYEONG_WINDOW}
    for i, uid in enumerate(others):
        dates[uid] = FESTIVAL_WINDOWS[i % len(FESTIVAL_WINDOWS)]
    return dates


def build_baseline() -> dict:
    ids = max_ids()
    return {
        "captured_at": datetime.now().isoformat(timespec="seconds"),
        "legacy_max_ids": ids,
        "legacy_order_count": query_int("SELECT COUNT(*) FROM orders"),
        "legacy_order_total_price_sum": query_int("SELECT COALESCE(SUM(total_order_price), 0) FROM orders"),
        "idempotency_count": query_int("SELECT COUNT(*) FROM idempotency"),
        "outbox_count": query_int("SELECT COUNT(*) FROM outbox"),
    }


def write_csv(name: str, header: list[str], rows: list[list]) -> None:
    with open(FIXTURES / name, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(header)
        w.writerows(rows)


def main() -> None:
    parser = argparse.ArgumentParser(description="주문 적재 사전 준비 (fixture 생성 + 부스 OPEN)")
    parser.add_argument("--seed", type=int, default=BASE_SEED)
    parser.add_argument("--force", action="store_true", help="이미 있는 fixture를 다시 만든다")
    parser.add_argument("--skip-booth-update", action="store_true", help="부스 OPEN 업데이트를 건너뛴다")
    args = parser.parse_args()

    if (FIXTURES / "baseline.json").exists() and not args.force:
        fail("fixture가 이미 있습니다. 다시 만들려면 --force (기준값이 바뀌므로 적재 전 상태에서만 쓴다)")
    FIXTURES.mkdir(parents=True, exist_ok=True)

    groups = build_groups()
    booth_university = build_booth_university()
    menus = build_menus(booth_university)
    menu_qty = qty_by_menu()
    booth_orders = orders_by_booth()
    rep = build_rep_menus(menus, booth_university, menu_qty)
    popular = build_popular_booths(menus, booth_university, booth_orders)
    festival = build_festival_dates(groups, random.Random(args.seed))
    baseline = build_baseline()
    if baseline["legacy_order_count"] > 0 and any(booth_university.get(b) != SEOKYEONG_ID for b in booth_orders):
        raise ToolError("서경대 외 부스에 이미 주문이 있습니다 — 적재 전 상태가 아닙니다")

    write_csv("groups.csv", ["university_id", "group"], [[u, g] for u, g in sorted(groups.items())])
    write_csv("booths.csv", ["booth_id", "university_id"], [[b, u] for b, u in sorted(booth_university.items())])
    write_csv("menus.csv", ["booth_id", "menu_id", "price", "is_rep"],
              [[b, m, p, 1 if m in rep[b] else 0] for b in sorted(menus) for m, p in menus[b]])
    write_csv("popular_booths.csv", ["university_id", "booth_id"],
              [[u, b] for u in sorted(popular) for b in popular[u]])
    write_csv("festival_dates.csv", ["university_id", "day1", "day2", "day3"],
              [[u, *festival[u]] for u in sorted(festival)])
    with open(FIXTURES / "baseline.json", "w", encoding="utf-8") as f:
        json.dump(baseline, f, ensure_ascii=False, indent=2)

    counts = {g: sum(1 for b in menus if groups[booth_university[b]] == g) for g in ("HOT", "WARM", "ETC")}
    print(f"[prepare] 학교 그룹: { {g: list(groups.values()).count(g) for g in ('HOT', 'WARM', 'ETC')} }")
    print(f"[prepare] 신규 주문 대상 부스: {counts} (전체 부스 {len(booth_university)})")
    print(f"[prepare] 인기 부스: {popular}")
    print(f"[prepare] 기준값: {baseline}")

    if not args.skip_booth_update:
        sec = execute("UPDATE booth SET booth_status = 'OPEN', order_enabled = 1")
        opened = query_int("SELECT COUNT(*) FROM booth WHERE booth_status = 'OPEN' AND order_enabled = 1")
        print(f"[prepare] 부스 OPEN + order_enabled=1: {opened}개 ({sec:.2f}s)")


if __name__ == "__main__":
    try:
        main()
    except ToolError as e:
        fail(str(e))
