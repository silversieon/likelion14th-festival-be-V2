"""[4] 단계 검증 (LLD-0006 13.2) — V1~V18을 확인하고 하나라도 실패하면 종료 코드 1.

사용: python verify.py --target 50000
"""

from __future__ import annotations

import argparse
import sys
import time

from common import (GROUP_WEIGHTS, GROUPS, MENU_COUNT_WEIGHTS, MENU_COUNTS, STATUS_WEIGHTS, STATUSES, ToolError,
                    fail, largest_remainder, load_fixtures, menu_count_distribution, orders_by_booth, qty_by_booth,
                    qty_by_menu, query, query_int, status_distribution)

results: list[tuple[str, str, str, bool]] = []


def check(code: str, name: str, actual: str, ok: bool) -> None:
    results.append((code, name, actual, ok))


def pct(part: int, whole: int) -> float:
    return 100.0 * part / whole if whole else 0.0


def main() -> None:
    parser = argparse.ArgumentParser(description="주문 적재 검증")
    parser.add_argument("--target", type=int, required=True)
    args = parser.parse_args()
    started = time.perf_counter()

    fx = load_fixtures()
    legacy = fx.baseline["legacy_max_ids"]
    lo_order, lo_item = legacy["orders"], legacy["order_items"]

    total = query_int("SELECT COUNT(*) FROM orders")
    check("V1", "orders 누적 건수", f"{total:,} (목표 {args.target:,})", total == args.target)

    obb = orders_by_booth()
    uni_count: dict[int, int] = {}
    for b, c in obb.items():
        uni_count[fx.booth_university[b]] = uni_count.get(fx.booth_university[b], 0) + c
    group_count = {g: sum(c for u, c in uni_count.items() if fx.groups[u] == g) for g in GROUPS}
    for g, w in zip(GROUPS, GROUP_WEIGHTS):
        share = pct(group_count[g], total)
        check("V2", f"그룹 비중 {g}", f"{share:.3f}% (목표 {w}%)", abs(share - w) <= 0.05)

    hot_unis = sorted(fx.popular)
    for uid in hot_unis:
        share = pct(uni_count.get(uid, 0), total)
        check("V3", f"HOT 학교 {uid} 비중", f"{share:.3f}% (목표 10%)", abs(share - 10) <= 0.05)
        pop = sum(obb.get(b, 0) for b in fx.popular[uid])
        pop_share = pct(pop, uni_count.get(uid, 0))
        check("V4", f"HOT 학교 {uid} 인기 3곳 합", f"{pop_share:.3f}% (목표 70%)", abs(pop_share - 70) <= 0.1)

    eligible_by_uni: dict[int, list[int]] = {}
    for b in fx.menus:
        eligible_by_uni.setdefault(fx.booth_university[b], []).append(b)
    for g in ("WARM", "ETC"):
        unis = [u for u, gg in fx.groups.items() if gg == g]
        counts = [uni_count.get(u, 0) for u in unis]
        check("V5", f"{g} 학교 간 주문 수 편차", f"최소 {min(counts):,} / 최대 {max(counts):,}",
              max(counts) - min(counts) <= 1)
        worst = 0
        for u in unis:
            booth_counts = [obb.get(b, 0) for b in eligible_by_uni.get(u, [])]
            if booth_counts:
                worst = max(worst, max(booth_counts) - min(booth_counts))
        check("V6", f"{g} 학교 안 부스 간 편차(최대)", f"{worst}", worst <= 1)

    mq = qty_by_menu()
    qb = qty_by_booth()
    rep_qty = {b: sum(mq.get(m.menu_id, 0) for m in ms if m.is_rep) for b, ms in fx.menus.items()}
    eligible_total = sum(qb.get(b, 0) for b in fx.menus)
    share = pct(sum(rep_qty.values()), eligible_total)
    check("V7", "대표 메뉴 수량 비중 (대상 부스 합산)", f"{share:.3f}% (목표 65%)", abs(share - 65) <= 0.5)
    for uid in hot_unis:
        for b in fx.popular[uid]:
            s = pct(rep_qty[b], qb.get(b, 0))
            check("V8", f"인기 부스 {b} 대표 메뉴 수량 비중", f"{s:.2f}%", abs(s - 65) <= 2)

    statuses = status_distribution()
    targets = dict(zip(STATUSES, largest_remainder(total, STATUS_WEIGHTS)))
    for s, w in zip(STATUSES, STATUS_WEIGHTS):
        check("V9", f"상태 {s}", f"{statuses.get(s, 0):,} = {pct(statuses.get(s, 0), total):.3f}% (목표 {w}%)",
              statuses.get(s, 0) == targets[s])

    kd = menu_count_distribution()
    for k, w in zip(MENU_COUNTS, MENU_COUNT_WEIGHTS):
        s = pct(kd.get(k, 0), total)
        check("V10", f"메뉴 {k}개 주문", f"{s:.3f}% (목표 {w}%)", abs(s - w) <= 0.1)

    v11 = query_int(f"""
        SELECT (SELECT COUNT(*) FROM order_items oi JOIN booth_menu bm ON bm.id = oi.booth_menu_id
                WHERE oi.id > {lo_item} AND (oi.menu_price <> bm.price OR oi.total_order_item_price <> oi.quantity * oi.menu_price))
             + (SELECT COUNT(*) FROM orders o JOIN (SELECT order_id, SUM(total_order_item_price) s FROM order_items GROUP BY order_id) t
                ON t.order_id = o.id WHERE o.id > {lo_order} AND o.total_order_price <> t.s)""")
    check("V11", "가격 정합성 위반", f"{v11}", v11 == 0)

    v12 = query_int(f"""
        SELECT COUNT(*) FROM order_items oi
        LEFT JOIN (SELECT order_item_id, COUNT(*) c FROM order_item_units GROUP BY order_item_id) u ON u.order_item_id = oi.id
        WHERE oi.id > {lo_item} AND COALESCE(u.c, 0) <> oi.quantity""")
    check("V12", "단위 수 ≠ 수량인 항목", f"{v12}", v12 == 0)

    multi = query_int("""
        SELECT COUNT(*) FROM (SELECT oi.order_id FROM order_items oi JOIN booth_menu bm ON bm.id = oi.booth_menu_id
        GROUP BY oi.order_id HAVING COUNT(DISTINCT bm.booth_id) > 1) t""")
    dup = query_int("""
        SELECT COUNT(*) FROM (SELECT order_id FROM order_items GROUP BY order_id, booth_menu_id HAVING COUNT(*) > 1) t""")
    check("V13", "여러 부스 주문 / 중복 메뉴 주문", f"{multi} / {dup}", multi == 0 and dup == 0)

    v14_cols = query_int(f"""
        SELECT COUNT(*) FROM orders WHERE id > {lo_order} AND NOT (
          (order_status = 'WAITING' AND completed_at IS NULL AND canceled_at IS NULL AND order_cancel_reason IS NULL AND modified_at = created_at)
          OR (order_status = 'COOKING' AND completed_at IS NULL AND canceled_at IS NULL AND order_cancel_reason IS NULL AND modified_at > created_at)
          OR (order_status = 'COMPLETED' AND completed_at > created_at AND canceled_at IS NULL AND order_cancel_reason IS NULL AND modified_at = completed_at)
          OR (order_status = 'CANCELED' AND completed_at IS NULL AND canceled_at > created_at AND order_cancel_reason IS NOT NULL AND modified_at = canceled_at))""")
    v14_served = query_int(f"""
        SELECT COUNT(*) FROM (
          SELECT o.order_status st, SUM(u.is_served) s, COUNT(*) n
          FROM orders o JOIN order_items oi ON oi.order_id = o.id JOIN order_item_units u ON u.order_item_id = oi.id
          WHERE o.id > {lo_order} GROUP BY o.id, o.order_status) t
        WHERE (st = 'COMPLETED' AND s <> n) OR (st IN ('WAITING', 'CANCELED') AND s <> 0) OR (st = 'COOKING' AND s >= n)""")
    check("V14", "상태별 컬럼 / is_served 규칙 위반", f"{v14_cols} / {v14_served}", v14_cols == 0 and v14_served == 0)

    values = ",".join(f"({u},'{d[0]}','{d[1]}','{d[2]}')" for u, d in fx.festival.items())
    v15 = int(query(f"""
        CREATE TEMPORARY TABLE fd (university_id BIGINT PRIMARY KEY, d1 DATE, d2 DATE, d3 DATE);
        INSERT INTO fd VALUES {values};
        SELECT COUNT(DISTINCT o.id) FROM orders o
        JOIN order_items oi ON oi.order_id = o.id JOIN booth_menu bm ON bm.id = oi.booth_menu_id
        JOIN booth b ON b.id = bm.booth_id JOIN departments d ON d.id = b.department_id
        JOIN fd ON fd.university_id = d.university_id
        WHERE o.id > {lo_order} AND DATE(o.created_at) NOT IN (fd.d1, fd.d2, fd.d3);""")[0][0])
    check("V15", "축제 3일 밖에 생성된 신규 주문", f"{v15}", v15 == 0)

    legacy_count = query_int(f"SELECT COUNT(*) FROM orders WHERE id <= {lo_order}")
    legacy_sum = query_int(f"SELECT COALESCE(SUM(total_order_price), 0) FROM orders WHERE id <= {lo_order}")
    check("V16", "기존 주문 건수 / 합계", f"{legacy_count:,} / {legacy_sum:,}",
          legacy_count == fx.baseline["legacy_order_count"] and legacy_sum == fx.baseline["legacy_order_total_price_sum"])

    idem = query_int("SELECT COUNT(*) FROM idempotency")
    outbox = query_int("SELECT COUNT(*) FROM outbox")
    check("V17", "idempotency / outbox 행 수", f"{idem} / {outbox} (기준 {fx.baseline['idempotency_count']} / "
          f"{fx.baseline['outbox_count']})",
          idem == fx.baseline["idempotency_count"] and outbox == fx.baseline["outbox_count"])

    v18 = query_int(f"""
        SELECT COUNT(*) FROM order_items oi JOIN booth_menu bm ON bm.id = oi.booth_menu_id
        WHERE oi.id > {lo_item} AND (bm.is_sold_out = 1 OR bm.price = 0)""")
    check("V18", "품절·0원 메뉴를 쓴 신규 항목", f"{v18}", v18 == 0)

    width = max(len(r[1]) for r in results)
    for code, name, actual, ok in results:
        print(f"{'PASS' if ok else 'FAIL'}  {code:<4} {name:<{width}}  {actual}")
    failed = [r for r in results if not r[3]]
    print(f"\n[verify] {len(results) - len(failed)}/{len(results)} 통과 ({time.perf_counter() - started:.1f}s)")
    if failed:
        sys.exit(1)


if __name__ == "__main__":
    try:
        main()
    except ToolError as e:
        fail(str(e))
