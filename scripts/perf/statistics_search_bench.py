"""운영진 통계 검색 API 응답시간 측정 (LLD-0007 13.4, ADR-0005).

Nginx(:8888)를 거쳐 단일 클라이언트로 순차 호출하고 p50/p95/p99를 마크다운 표로 출력한다.
표준 라이브러리만 쓴다.

usage: python -I scripts/perf/statistics_search_bench.py <ADMIN_ACCESS_TOKEN> [warmup=5] [runs=30]

케이스
- Before와 같은 4개: universityName = 대학 / 서경대학교 × (booths/search, booths/menus)
- 접두 일치에서 가장 넓은 한 글자 "한": 첫 페이지, 그리고 첫 페이지의 nextCursor로 읽는 두 번째 페이지
응답이 List(Before)여도, CursorPageResponse(After)여도 행 수를 센다.
"""
import json
import statistics
import sys
import time
import urllib.parse
import urllib.request

BASE = "http://localhost:8888"
CASES = [
    ("booths/search", "대학", False),
    ("booths/search", "서경대학교", False),
    ("booths/search", "한", False),
    ("booths/search", "한", True),
    ("booths/menus", "대학", False),
    ("booths/menus", "서경대학교", False),
    ("booths/menus", "한", False),
    ("booths/menus", "한", True),
]


def call(token, path, params):
    url = f"{BASE}/api/statistics/{path}?{urllib.parse.urlencode(params)}"
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    start = time.perf_counter()
    with urllib.request.urlopen(req, timeout=60) as res:
        body = res.read()
    elapsed = (time.perf_counter() - start) * 1000
    data = json.loads(body)["data"]
    if isinstance(data, dict):
        return elapsed, len(data["items"]), len(body), data.get("nextCursor")
    return elapsed, len(data), len(body), None


def pct(values, p):
    values = sorted(values)
    k = (len(values) - 1) * p / 100
    lo, hi = int(k), min(int(k) + 1, len(values) - 1)
    return values[lo] + (values[hi] - values[lo]) * (k - lo)


def main():
    token = sys.argv[1]
    warmup = int(sys.argv[2]) if len(sys.argv) > 2 else 5
    runs = int(sys.argv[3]) if len(sys.argv) > 3 else 30
    print("| API | universityName | 페이지 | 응답 행 | 본문 | p50 (ms) | p95 (ms) | p99 (ms) |")
    print("|---|---|---|---|---|---|---|---|")
    for path, keyword, second_page in CASES:
        params = {"universityName": keyword}
        if second_page:
            _, _, _, cursor = call(token, path, params)
            if cursor is None:
                print(f"| {path} | {keyword} | 2 | (다음 페이지 없음) | | | | |")
                continue
            params["encodedCursor"] = cursor
        for _ in range(warmup):
            call(token, path, params)
        times, rows, size = [], 0, 0
        for _ in range(runs):
            t, rows, size, _ = call(token, path, params)
            times.append(t)
        print(
            f"| {path} | {keyword} | {2 if second_page else 1} | {rows:,} | {size / 1024:,.0f} KB | "
            f"{statistics.median(times):,.0f} | {pct(times, 95):,.0f} | {pct(times, 99):,.0f} |"
        )


if __name__ == "__main__":
    main()
