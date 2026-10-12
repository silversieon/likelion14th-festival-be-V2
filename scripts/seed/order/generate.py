"""[2] 단계 생성 (LLD-0006 6장) — 누적 목표 N까지 모자란 만큼만 주문을 만들어 TSV로 쓴다.

모든 배분은 "목표 누적 − 현재 DB 건수"로 계산한다. 같은 DB 상태 + 같은 시드면 같은 파일이 나온다.

사용: python generate.py --target 50000 [--seed N] [--chunk 100000] [--out DIR] [--force]
"""

from __future__ import annotations

import argparse
import json
import math
import random
import shutil
import time
from array import array
from bisect import bisect_right
from dataclasses import dataclass, field
from datetime import datetime, time as dtime, timedelta
from itertools import accumulate
from pathlib import Path

from common import (BASE_SEED, GROUP_WEIGHTS, GROUPS, MENU_COUNT_WEIGHTS, MENU_COUNTS, OUT, POPULAR_SHARE,
                    REP_SHARE, STATUS_WEIGHTS, STATUSES, Fixtures, ToolError, fail, largest_remainder,
                    load_fixtures, max_ids, menu_count_distribution, orders_by_booth, qty_by_booth, qty_by_menu, query_int,
                    status_distribution, water_fill)

# 서경대 축제 기간(5/13~15) 실측 — LLD-0006 6.3, 6.5
DAY_WEIGHTS = (20, 42, 38)
HOUR_WEIGHTS = {10: 0.1, 11: 0.8, 12: 1.3, 13: 0.4, 14: 1.7, 15: 0.8, 16: 2.0, 17: 7.8, 18: 31.6,
                19: 25.7, 20: 17.5, 21: 6.0, 22: 3.9, 23: 0.5}
QTY_VALUES = (1, 2, 3, 4, 5, 10)
QTY_WEIGHTS = (93.04, 5.68, 0.89, 0.32, 0.03, 0.03)
QTY_MEAN = sum(v * w for v, w in zip(QTY_VALUES, QTY_WEIGHTS)) / sum(QTY_WEIGHTS)
CANCEL_REASONS = ("ORDER_MISTAKE", "ETC", "OUT_OF_STOCK", "CUSTOMER_REQUEST")
SURNAMES = ("김", "이", "박", "최", "정", "강", "조", "윤", "장", "임", "한", "오", "서", "신", "권", "황", "안", "송",
            "류", "홍")
GIVEN_NAMES = ("민준", "서연", "도윤", "서윤", "시우", "지우", "하준", "하윤", "주원", "지민", "지호", "수아", "준서",
               "채원", "예준", "지유", "유준", "다은", "건우", "은서", "현우", "예은", "선우", "수빈", "연우")
NULL = "\\N"


@dataclass
class Snapshot:
    total_orders: int
    orders_by_booth: dict[int, int]
    status_counts: dict[str, int]
    k_counts: dict[int, int]
    qty_by_menu: dict[int, int]
    qty_by_booth: dict[int, int]
    max_ids: dict[str, int]


@dataclass
class Plan:
    booth_new: dict[int, int]
    status_new: dict[str, int]
    k_new: dict[int, int]
    summary: dict = field(default_factory=dict)


def read_snapshot() -> Snapshot:
    return Snapshot(
        total_orders=query_int("SELECT COUNT(*) FROM orders"),
        orders_by_booth=orders_by_booth(),
        status_counts=status_distribution(),
        k_counts=menu_count_distribution(),
        qty_by_menu=qty_by_menu(),
        qty_by_booth=qty_by_booth(),
        max_ids=max_ids(),
    )


def plan_booth_quotas(target: int, snap: Snapshot, fx: Fixtures) -> tuple[dict[int, int], dict]:
    """그룹 → 학교 → 부스 순으로 누적 목표를 정하고, 모자란 만큼을 물 채우기로 배정한다 (LLD-0006 6.2)."""
    unis_by_group = {g: sorted(u for u, gg in fx.groups.items() if gg == g) for g in GROUPS}
    eligible_by_uni: dict[int, list[int]] = {}
    for booth_id in sorted(fx.menus):
        eligible_by_uni.setdefault(fx.booth_university[booth_id], []).append(booth_id)
    uni_current: dict[int, int] = {}
    for booth_id, count in snap.orders_by_booth.items():
        uid = fx.booth_university[booth_id]
        uni_current[uid] = uni_current.get(uid, 0) + count
    unattributed = snap.total_orders - sum(snap.orders_by_booth.values())

    group_target = dict(zip(GROUPS, largest_remainder(target, GROUP_WEIGHTS)))
    group_current = {g: sum(uni_current.get(u, 0) for u in unis_by_group[g]) for g in GROUPS}
    group_need = {g: group_target[g] - group_current[g] for g in GROUPS}
    group_need["ETC"] -= unattributed  # 항목 없는 기존 주문은 ETC 몫에서 차감해 누적 합계를 맞춘다
    for g, need in group_need.items():
        if need < 0:
            raise ToolError(f"그룹 {g}의 기존 주문이 목표를 넘습니다 (목표 {group_target[g]}, 현재 {group_current[g]})")

    # 칸의 목표는 "이번 단계가 끝났을 때 그 칸이 갖게 될 건수(현재 + 배정)"를 균등 분할한 값으로 잡는다.
    # 그래야 항목 없는 기존 주문 차감·기존 편차가 있어도 칸 사이 차이가 1 이내로 유지된다.
    booth_new: dict[int, int] = {}
    for g in GROUPS:
        unis = unis_by_group[g]
        currents = [uni_current.get(u, 0) for u in unis]
        school_target = largest_remainder(sum(currents) + group_need[g], [1] * len(unis))
        school_new = water_fill(group_need[g], school_target, currents)
        for uid, n_school in zip(unis, school_new):
            if n_school == 0:
                continue
            eligible = eligible_by_uni.get(uid, [])
            if not eligible:
                raise ToolError(f"학교 {uid}에 신규 주문 대상 부스가 없습니다")
            if g == "HOT":
                popular = fx.popular[uid]
                others = [b for b in eligible if b not in popular]
                pop_target, oth_target = largest_remainder(uni_current.get(uid, 0) + n_school, POPULAR_SHARE)
                cur_pop = sum(snap.orders_by_booth.get(b, 0) for b in popular)
                cur_oth = uni_current.get(uid, 0) - cur_pop  # 신규 대상에서 빠진 부스의 기존 주문 포함
                n_pop, n_oth = water_fill(n_school, [pop_target, oth_target], [cur_pop, cur_oth])
                cells = [(popular, n_pop), (others, n_oth)]
            else:
                cells = [(eligible, n_school)]
            for booths, n_cell in cells:
                cur = [snap.orders_by_booth.get(b, 0) for b in booths]
                targets = largest_remainder(sum(cur) + n_cell, [1] * len(booths))
                alloc = water_fill(n_cell, targets, cur)
                for b, n in zip(booths, alloc):
                    if n:
                        booth_new[b] = booth_new.get(b, 0) + n

    summary = {"group_target": group_target, "group_current": group_current, "group_new": group_need,
               "unattributed_legacy_orders": unattributed}
    return booth_new, summary


def plan_status_quotas(target: int, snap: Snapshot) -> dict[str, int]:
    unknown = set(snap.status_counts) - set(STATUSES)
    if unknown:
        raise ToolError(f"알 수 없는 주문 상태가 있습니다: {unknown}")
    targets = dict(zip(STATUSES, largest_remainder(target, STATUS_WEIGHTS)))
    need = {s: targets[s] - snap.status_counts.get(s, 0) for s in STATUSES}
    if any(v < 0 for v in need.values()):
        raise ToolError(f"상태 목표보다 기존 주문이 많습니다: {need}")
    return need


def plan_k_quotas(target: int, snap: Snapshot) -> dict[int, int]:
    targets = dict(zip(MENU_COUNTS, largest_remainder(target, MENU_COUNT_WEIGHTS)))
    legacy_other = sum(c for k, c in snap.k_counts.items() if k not in MENU_COUNTS)
    targets[3] -= legacy_other  # 기존 0개·5개 이상 주문은 3개 목표에서 차감 (LLD-0006 6.5)
    need = {k: targets[k] - snap.k_counts.get(k, 0) for k in MENU_COUNTS}
    if any(v < 0 for v in need.values()):
        raise ToolError(f"메뉴 수 목표보다 기존 주문이 많습니다: {need}")
    return need


def build_plan(target: int, snap: Snapshot, fx: Fixtures) -> Plan:
    booth_new, summary = plan_booth_quotas(target, snap, fx)
    status_new = plan_status_quotas(target, snap)
    k_new = plan_k_quotas(target, snap)
    new_total = target - snap.total_orders
    for name, quota in (("부스", booth_new), ("상태", status_new), ("메뉴 수", k_new)):
        if sum(quota.values()) != new_total:
            raise ToolError(f"{name} 쿼터 합 {sum(quota.values())} ≠ 생성 건수 {new_total}")
    return Plan(booth_new, status_new, k_new, summary)


def assign(plan: Plan, fx: Fixtures, rng: random.Random) -> tuple[array, array, array]:
    """부스·상태·메뉴 수 목록을 정확한 건수로 만들고 섞어서 주문 순서대로 짝짓는다."""
    booth_arr = array("I")
    for b in sorted(plan.booth_new):
        booth_arr.extend(array("I", [b]) * plan.booth_new[b])
    status_arr = array("B")
    for code, s in enumerate(STATUSES):
        status_arr.extend(array("B", [code]) * plan.status_new[s])
    k_arr = array("B")
    for k in MENU_COUNTS:
        k_arr.extend(array("B", [k]) * plan.k_new[k])
    for arr in (booth_arr, status_arr, k_arr):
        rng.shuffle(arr)

    # 메뉴가 3개뿐인 부스에 4개짜리 주문이 걸리면, 다른 부스의 작은 k와 맞바꾼다 (총 건수 유지)
    menu_count = {b: len(m) for b, m in fx.menus.items()}
    j = 0
    for i in range(len(k_arr)):
        mi = menu_count[booth_arr[i]]
        if k_arr[i] <= mi:
            continue
        while j < len(k_arr) and not (k_arr[j] <= mi and k_arr[i] <= menu_count[booth_arr[j]]):
            j += 1
        if j >= len(k_arr):
            raise ToolError("메뉴 수를 맞바꿀 주문을 찾지 못했습니다")
        k_arr[i], k_arr[j] = k_arr[j], k_arr[i]
    return booth_arr, status_arr, k_arr


def rep_bounds(k: int, menu_total: int) -> tuple[int, int]:
    """주문 하나에 들어갈 수 있는 대표 메뉴 수의 (최소, 최대)."""
    return max(0, k - (menu_total - 2)), min(k, 2)


def plan_rep_reductions(booth_arr: array, k_arr: array, snap: Snapshot, fx: Fixtures) -> dict[int, list[int]]:
    """부스마다 '대표 메뉴를 하나 덜 넣는 주문' 수를 정해 판매 수량 비중을 65%에 맞춘다 (LLD-0006 6.4).

    반환: booth_id → [줄일 주문 수, 줄일 수 있는 주문 수]
    """
    sum_rmax: dict[int, int] = {}
    sum_k: dict[int, int] = {}
    reducible: dict[int, int] = {}
    menu_total = {b: len(m) for b, m in fx.menus.items()}
    for b, k in zip(booth_arr, k_arr):
        lo, hi = rep_bounds(k, menu_total[b])
        sum_rmax[b] = sum_rmax.get(b, 0) + hi
        sum_k[b] = sum_k.get(b, 0) + k
        if hi - 1 >= lo:
            reducible[b] = reducible.get(b, 0) + 1
    plan: dict[int, list[int]] = {}
    fresh: list[tuple[int, float]] = []  # 기존 주문이 없는 부스 (줄일 수, 원하는 감소 수)
    for b in sorted(sum_k):
        rep0 = sum(snap.qty_by_menu.get(m.menu_id, 0) for m in fx.menus[b] if m.is_rep)
        total0 = snap.qty_by_booth.get(b, 0)
        # 기존 + 신규 기대 수량에서 대표 비중이 REP_SHARE가 되도록 하는 대표 항목 수
        target_lines = (REP_SHARE * (total0 + QTY_MEAN * sum_k[b]) - rep0) / QTY_MEAN
        desired = sum_rmax[b] - target_lines
        cap = reducible.get(b, 0)
        if total0 > 0:  # 기존 주문이 있는 부스(서경대)는 부스 단위로 최대한 맞춘다
            plan[b] = [min(max(0, round(desired)), cap), cap]
        else:
            fresh.append((b, desired))
            plan[b] = [0, cap]
    # 기존 주문이 없는 부스는 감소 수를 부스마다 반올림하면 주문 1~2건짜리 부스에서 손실이 쌓인다.
    # 전체 합을 먼저 정하고(원하는 감소 수의 합) 부스별 원하는 값에 비례해 최대 잉여 방식으로 나눈다.
    weights = [min(max(0.0, d), plan[b][1]) for b, d in fresh]
    total = min(max(0, round(sum(d for _, d in fresh))), math.floor(sum(weights)))
    for (b, _), n in zip(fresh, largest_remainder(total, weights) if fresh else []):
        plan[b][0] = min(n, plan[b][1])
    return plan


def fmt(dt: datetime) -> str:
    return dt.strftime("%Y-%m-%d %H:%M:%S.%f")


def generate(target: int, seed: int, chunk: int, out: Path, force: bool) -> dict:
    started = time.perf_counter()
    fx = load_fixtures()
    snap = read_snapshot()
    if target < snap.total_orders:
        raise ToolError(f"목표 {target}가 현재 주문 수 {snap.total_orders}보다 작습니다 — 롤백을 먼저 하세요")
    new_total = target - snap.total_orders
    if new_total == 0:
        print(f"[generate] 이미 목표 {target}건입니다. 생성할 주문이 없습니다")
        return {}
    if out.exists() and any(out.iterdir()):
        if not force:
            raise ToolError(f"출력 폴더가 비어 있지 않습니다: {out} (--force로 덮어쓰기)")
        shutil.rmtree(out)
    out.mkdir(parents=True, exist_ok=True)

    rng = random.Random(seed)
    plan = build_plan(target, snap, fx)
    booth_arr, status_arr, k_arr = assign(plan, fx, rng)
    reductions = plan_rep_reductions(booth_arr, k_arr, snap, fx)

    rep_menus = {b: [m for m in ms if m.is_rep] for b, ms in fx.menus.items()}
    other_menus = {b: [m for m in ms if not m.is_rep] for b, ms in fx.menus.items()}
    day_cum = list(accumulate(DAY_WEIGHTS))
    hours = list(HOUR_WEIGHTS)
    hour_cum = list(accumulate(HOUR_WEIGHTS.values()))
    qty_cum = list(accumulate(QTY_WEIGHTS))
    names = [s + g for s in SURNAMES for g in GIVEN_NAMES]
    day_starts = {uid: [datetime.combine(d, dtime(0)) for d in days] for uid, days in fx.festival.items()}

    order_id = snap.max_ids["orders"]
    item_id = snap.max_ids["order_items"]
    unit_id = snap.max_ids["order_item_units"]
    checks = {"status": dict.fromkeys(STATUSES, 0), "k": dict.fromkeys(MENU_COUNTS, 0), "booth": {}}
    chunks = []

    for c_index, c_start in enumerate(range(0, new_total, chunk)):
        c_end = min(new_total, c_start + chunk)
        o_lines, i_lines, u_lines = [], [], []
        first = (order_id + 1, item_id + 1, unit_id + 1)
        for i in range(c_start, c_end):
            booth_id = booth_arr[i]
            status = STATUSES[status_arr[i]]
            k = k_arr[i]
            order_id += 1
            checks["status"][status] += 1
            checks["k"][k] += 1
            checks["booth"][booth_id] = checks["booth"].get(booth_id, 0) + 1

            # 시각
            uid = fx.booth_university[booth_id]
            day = day_starts[uid][bisect_right(day_cum, rng.random() * day_cum[-1])]
            hour = hours[bisect_right(hour_cum, rng.random() * hour_cum[-1])]
            created = day + timedelta(hours=hour, seconds=rng.randrange(3600), microseconds=rng.randrange(1_000_000))
            completed = canceled = reason = NULL
            if status == "WAITING":
                modified = created
            elif status == "COOKING":
                modified = created + timedelta(seconds=rng.randint(60, 900))
            elif status == "COMPLETED":
                modified = created + timedelta(seconds=rng.randint(300, 2400))
                completed = fmt(modified)
            else:
                modified = created + timedelta(seconds=rng.randint(60, 1800))
                canceled = fmt(modified)
                reason = CANCEL_REASONS[rng.randrange(4)]

            # 메뉴: 대표 메뉴 수 r을 정하고 나머지는 중복 없이 고른다
            lo, hi = rep_bounds(k, len(fx.menus[booth_id]))
            r = hi
            if hi - 1 >= lo:
                red = reductions[booth_id]
                if red[0] > 0 and rng.random() < red[0] / red[1]:
                    r = hi - 1
                    red[0] -= 1
                red[1] -= 1
            reps = rep_menus[booth_id]
            chosen = list(reps) if r == 2 else ([reps[rng.randrange(2)]] if r == 1 else [])
            chosen += rng.sample(other_menus[booth_id], k - r)

            total = 0
            unit_flags = []
            for menu in chosen:
                qty = QTY_VALUES[bisect_right(qty_cum, rng.random() * qty_cum[-1])]
                item_id += 1
                line_total = qty * menu.price
                total += line_total
                i_lines.append(f"{item_id}\t{qty}\t{menu.price}\t{line_total}\t{order_id}\t{menu.menu_id}\n")
                unit_flags.extend([item_id] * qty)
            if status == "COMPLETED":
                served = set(range(len(unit_flags)))
            elif status == "COOKING":
                served = set(rng.sample(range(len(unit_flags)), rng.randrange(len(unit_flags))))
            else:
                served = set()
            for pos, owner in enumerate(unit_flags):
                unit_id += 1
                u_lines.append(f"{unit_id}\t{1 if pos in served else 0}\t{owner}\n")

            phone = f"010-{rng.randrange(10000):04d}-{rng.randrange(10000):04d}"
            o_lines.append(f"{order_id}\t{rng.randint(1, 30)}\t{rng.randint(1, 6)}\t{names[rng.randrange(500)]}\t"
                           f"{phone}\t{total}\t{completed}\t{canceled}\t{status}\t{reason}\t{fmt(created)}\t"
                           f"{fmt(modified)}\n")

        files = {}
        for table, lines in (("orders", o_lines), ("order_items", i_lines), ("order_item_units", u_lines)):
            name = f"{table}_{c_index:03d}.tsv"
            with open(out / name, "w", encoding="utf-8", newline="\n") as f:
                f.writelines(lines)
            files[table] = name
        chunks.append({
            "index": c_index,
            "files": files,
            "ranges": {"orders": [first[0], order_id], "order_items": [first[1], item_id],
                       "order_item_units": [first[2], unit_id]},
            "rows": {"orders": len(o_lines), "order_items": len(i_lines), "order_item_units": len(u_lines)},
        })
        print(f"[generate] 묶음 {c_index:03d}: 주문 {len(o_lines):,} / 항목 {len(i_lines):,} / 단위 {len(u_lines):,}")

    # 자체 검사 — 쿼터와 실제 생성 건수가 같은지 (LLD-0006 13.1)
    if checks["status"] != plan.status_new or checks["k"] != plan.k_new or checks["booth"] != plan.booth_new:
        shutil.rmtree(out)
        raise ToolError("생성 건수가 쿼터와 다릅니다 — 출력을 삭제했습니다")
    for chunk_info in chunks:
        for table, name in chunk_info["files"].items():
            with open(out / name, encoding="utf-8") as f:
                if sum(1 for _ in f) != chunk_info["rows"][table]:
                    shutil.rmtree(out)
                    raise ToolError(f"{name} 행 수가 기록과 다릅니다 — 출력을 삭제했습니다")

    manifest = {
        "target": target,
        "seed": seed,
        "chunk_size": chunk,
        "generated_at": datetime.now().isoformat(timespec="seconds"),
        "snapshot": {"total_orders": snap.total_orders, "max_ids": snap.max_ids},
        "new_rows": {t: sum(c["rows"][t] for c in chunks) for t in ("orders", "order_items", "order_item_units")},
        "id_ranges": {t: [chunks[0]["ranges"][t][0], chunks[-1]["ranges"][t][1]]
                      for t in ("orders", "order_items", "order_item_units")},
        "quotas": {"status": plan.status_new, "menu_count": {str(k): v for k, v in plan.k_new.items()},
                   **plan.summary},
        "chunks": chunks,
        "elapsed_sec": round(time.perf_counter() - started, 2),
    }
    with open(out / "manifest.json", "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)
    print(f"[generate] 완료: {manifest['new_rows']} → {out} ({manifest['elapsed_sec']}s)")
    return manifest


def main() -> None:
    parser = argparse.ArgumentParser(description="누적 목표까지 주문 TSV 생성")
    parser.add_argument("--target", type=int, required=True, help="누적 주문 목표 (예: 50000)")
    parser.add_argument("--seed", type=int, help="기본값: 20261012 + target")
    parser.add_argument("--chunk", type=int, default=100_000, help="묶음당 주문 수")
    parser.add_argument("--out", type=Path, help="기본값: out/stage-<target>")
    parser.add_argument("--force", action="store_true", help="출력 폴더가 있으면 지우고 다시 만든다")
    args = parser.parse_args()
    seed = args.seed if args.seed is not None else BASE_SEED + args.target
    out = args.out or OUT / f"stage-{args.target:07d}"
    generate(args.target, seed, args.chunk, out, args.force)


if __name__ == "__main__":
    try:
        main()
    except ToolError as e:
        fail(str(e))
