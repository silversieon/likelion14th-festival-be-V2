"""[3] 단계 적재 (LLD-0006 7.2) — generate.py가 만든 묶음을 LOAD DATA INFILE로 넣는다.

묶음마다 orders → order_items → order_item_units 순서. 문장 하나 = 자동 커밋 하나. FK 검사는 켠 채로 둔다 (ADR-0004).

사용: python load.py out/stage-0050000 [--from-chunk N]
"""

from __future__ import annotations

import argparse
import json
import time
from pathlib import Path

from common import IN_CONTAINER_DIR, ToolError, docker_cp, docker_exec, execute, fail, max_ids, query_int

COLUMNS = {
    "orders": "(id, table_number, num_of_people, customer_name, customer_phone_number, total_order_price, "
              "completed_at, canceled_at, order_status, order_cancel_reason, created_at, modified_at)",
    "order_items": "(id, quantity, menu_price, total_order_item_price, order_id, booth_menu_id)",
    "order_item_units": "(id, is_served, order_item_id)",
}
TABLES = ("orders", "order_items", "order_item_units")


def load_sql(table: str, file_name: str) -> str:
    return (f"LOAD DATA INFILE '{IN_CONTAINER_DIR}/{file_name}' INTO TABLE {table} CHARACTER SET utf8mb4 "
            f"FIELDS TERMINATED BY '\\t' ESCAPED BY '\\\\' LINES TERMINATED BY '\\n' {COLUMNS[table]}")


def chunk_state(chunk: dict, current: dict[str, int]) -> str:
    """이미 들어감 / 아직 / 일부만 들어감."""
    states = set()
    for t in TABLES:
        start, end = chunk["ranges"][t]
        if current[t] >= end:
            states.add("done")
        elif current[t] == start - 1:
            states.add("todo")
        else:
            states.add("partial")
    if states == {"done"}:
        return "done"
    if states == {"todo"}:
        return "todo"
    return "partial"


def main() -> None:
    parser = argparse.ArgumentParser(description="단계 묶음을 LOAD DATA로 적재")
    parser.add_argument("stage_dir", type=Path)
    parser.add_argument("--from-chunk", type=int, default=0)
    args = parser.parse_args()

    with open(args.stage_dir / "manifest.json", encoding="utf-8") as f:
        manifest = json.load(f)
    report_path = args.stage_dir / "load-report.json"
    report = json.loads(report_path.read_text(encoding="utf-8")) if report_path.exists() else {"chunks": {}}
    docker_exec("mkdir", "-p", IN_CONTAINER_DIR)

    started = time.perf_counter()
    for chunk in manifest["chunks"]:
        idx = chunk["index"]
        if idx < args.from_chunk:
            continue
        state = chunk_state(chunk, max_ids())
        if state == "done":
            print(f"[load] 묶음 {idx:03d}: 이미 적재됨 — 건너뜀")
            continue
        if state == "partial":
            raise ToolError(f"묶음 {idx:03d}이 일부만 적재되었거나 다른 행과 id가 겹칩니다. "
                            f"python rollback.py {args.stage_dir} --chunk {idx} 후 --from-chunk {idx}로 다시 실행하세요")
        timings = {}
        for table in TABLES:
            file_name = chunk["files"][table]
            docker_cp(args.stage_dir / file_name, f"{IN_CONTAINER_DIR}/{file_name}")
            try:
                timings[table] = round(execute(load_sql(table, file_name)), 2)
            except ToolError as e:
                raise ToolError(f"묶음 {idx:03d} {table} 적재 실패: {e}") from e
            finally:
                docker_exec("rm", "-f", f"{IN_CONTAINER_DIR}/{file_name}")
            start, end = chunk["ranges"][table]
            loaded = query_int(f"SELECT COUNT(*) FROM {table} WHERE id BETWEEN {start} AND {end}")
            if loaded != chunk["rows"][table]:
                raise ToolError(f"묶음 {idx:03d} {table}: 기대 {chunk['rows'][table]}행, 실제 {loaded}행")
        report["chunks"][str(idx)] = {"seconds": timings, "rows": chunk["rows"]}
        report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"[load] 묶음 {idx:03d}: {chunk['rows']} {timings}")

    report["last_run_seconds"] = round(time.perf_counter() - started, 2)
    report["total_load_seconds"] = round(sum(sum(c["seconds"].values()) for c in report["chunks"].values()), 2)
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"[load] 완료 — LOAD DATA 합계 {report['total_load_seconds']}s, 이번 실행 {report['last_run_seconds']}s")


if __name__ == "__main__":
    try:
        main()
    except ToolError as e:
        fail(str(e))
