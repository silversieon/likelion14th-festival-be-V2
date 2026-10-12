"""[1] 학교별 축제 일정 적재 (LLD-0006 2.2, 6.3) — 1회 실행.

모든 부스 × 그 학교의 축제 3일을 booth_operation에 넣는다. 하루 종일 운영(ALL, 00:00~23:59).
UNIQUE(booth_id, operation_date)와 겹치는 행(서경대 기존 일정)은 LOAD DATA ... IGNORE로 건너뛴다.

사용: python load_operations.py
"""

from __future__ import annotations

from datetime import datetime

from common import IN_CONTAINER_DIR, OUT, ToolError, docker_cp, docker_exec, execute, fail, load_fixtures, query_int

FILE_NAME = "booth_operation.tsv"


def write_operations_tsv() -> int:
    fx = load_fixtures()
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S.000000")
    OUT.mkdir(parents=True, exist_ok=True)
    rows = 0
    with open(OUT / FILE_NAME, "w", encoding="utf-8", newline="\n") as f:
        for booth_id in sorted(fx.booth_university):
            for day in fx.festival[fx.booth_university[booth_id]]:
                f.write(f"{booth_id}\t{day.isoformat()}\tALL\t00:00:00\t\\N\t23:59:00\t{now}\t{now}\n")
                rows += 1
    return rows


def main() -> None:
    rows = write_operations_tsv()
    before = query_int("SELECT COUNT(*) FROM booth_operation")
    docker_exec("mkdir", "-p", IN_CONTAINER_DIR)
    docker_cp(OUT / FILE_NAME, f"{IN_CONTAINER_DIR}/{FILE_NAME}")
    sec = execute(f"""
        LOAD DATA INFILE '{IN_CONTAINER_DIR}/{FILE_NAME}' IGNORE INTO TABLE booth_operation
        CHARACTER SET utf8mb4 FIELDS TERMINATED BY '\\t' LINES TERMINATED BY '\\n'
        (booth_id, operation_date, time_type, day_open_time, night_open_time, close_time, created_at, modified_at)""")
    docker_exec("rm", "-f", f"{IN_CONTAINER_DIR}/{FILE_NAME}")
    after = query_int("SELECT COUNT(*) FROM booth_operation")
    print(f"[operations] 시도 {rows}행 → 추가 {after - before}행, 건너뜀 {rows - (after - before)}행 ({sec:.2f}s)")


if __name__ == "__main__":
    try:
        main()
    except ToolError as e:
        fail(str(e))
