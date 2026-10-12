"""단계·묶음 롤백 (LLD-0006 7.2) — manifest의 id 구간을 자식 테이블부터 5만 행씩 지운다.

CASCADE에 맡기지 않는다 (한 문장이 수백만 행을 잠그지 않게).
단계 롤백은 가장 최근 단계부터만 허용한다.

사용: python rollback.py out/stage-0050000 [--chunk N]
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from common import ToolError, execute, fail, max_ids, query_int

BATCH = 50_000
CHILD_FIRST = ("order_item_units", "order_items", "orders")


def main() -> None:
    parser = argparse.ArgumentParser(description="단계 또는 묶음 롤백")
    parser.add_argument("stage_dir", type=Path)
    parser.add_argument("--chunk", type=int, help="이 묶음만 지운다 (적재 실패 복구용)")
    args = parser.parse_args()

    with open(args.stage_dir / "manifest.json", encoding="utf-8") as f:
        manifest = json.load(f)
    if args.chunk is None:
        ranges = manifest["id_ranges"]
        current = max_ids()
        later = [t for t in CHILD_FIRST if current[t] > ranges[t][1]]
        if later:
            raise ToolError(f"이 단계 뒤에 적재된 행이 있습니다 ({later}) — 가장 최근 단계부터 롤백하세요")
    else:
        ranges = next(c["ranges"] for c in manifest["chunks"] if c["index"] == args.chunk)

    for table in CHILD_FIRST:
        start, end = ranges[table]
        before = query_int(f"SELECT COUNT(*) FROM {table} WHERE id BETWEEN {start} AND {end}")
        statements = [f"DELETE FROM {table} WHERE id BETWEEN {lo} AND {min(end, lo + BATCH - 1)};"
                      for lo in range(start, end + 1, BATCH)]
        sec = execute("\n".join(statements))
        after = query_int(f"SELECT COUNT(*) FROM {table} WHERE id BETWEEN {start} AND {end}")
        print(f"[rollback] {table} id {start}~{end}: {before:,} → {after:,} ({sec:.1f}s)")
        if after != 0:
            raise ToolError(f"{table} 구간에 행이 남았습니다: {after}")


if __name__ == "__main__":
    try:
        main()
    except ToolError as e:
        fail(str(e))
