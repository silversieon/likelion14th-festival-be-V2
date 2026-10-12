# 주문 대량 적재 도구 (핫스팟 분포)

설계: [LLD-0006](../../../docs/lld/LLD-0006-order-hotspot-bulk-load.md) · 결정: [ADR-0004](../../../docs/adr/ADR-0004-order-bulk-load-with-csv-load-data.md) · 이슈 #18

Python 생성기가 분포대로 주문을 만들어 탭 구분 파일(TSV)로 쓰고, `mysql` 컨테이너에 복사해 `LOAD DATA INFILE`로 넣는다.
누적 5만 → 10만 → 50만 → 100만 → 500만 건까지 단계별로 늘린다.

## 전제

- Docker Desktop 실행 중, `mysql` 컨테이너 기동 (`docker info`, `docker ps`)
- Python 3.11 이상 (표준 라이브러리만 사용)
- 명령은 이 폴더(`scripts/seed/order/`)에서 실행한다

## 실행 순서

```bash
# [0] 사전 준비 (1회) — fixtures/ 생성 + 모든 부스 OPEN, order_enabled=1
python prepare.py

# [1] 학교별 축제 일정 (1회) — booth_operation, 모든 부스 × 3일, 하루 종일
python load_operations.py

# [2]~[4] 단계마다 반복 (N = 50000, 100000, 500000, 1000000, 5000000)
python generate.py --target N          # out/stage-000N/ 에 TSV + manifest.json
python load.py out/stage-000N          # 묶음(주문 10만 건)마다 orders → order_items → order_item_units
python verify.py --target N            # V1~V18, 실패 시 종료 코드 1
```

결과(`manifest.json`의 건수·id 구간, `load-report.json`의 소요 시간, `verify.py` 출력)는 이슈 #18과 ADR-0004 결과 표에 남긴다.

## 단계 진행 현황

| 단계 | 누적 주문 | 상태 | 비고 |
|---|---|---|---|
| 1 | 50,000 | ✅ 적재 (2026-10-12) | 48/50 통과 — V7·V8(부스 18)은 서경대 기존 주문 영향, 승인된 예외 (LLD-0006 Q5) |
| 2 | 100,000 | ⬜ | 여기서부터 V7·V8 전부 통과해야 한다 |
| 3 | 500,000 | ⬜ | |
| 4 | 1,000,000 | ⬜ | |
| 5 | 5,000,000 | ⬜ | 생성 파일 약 1GB, 생성 수 분 · 적재 수~수십 분 예상 |

단계를 끝낼 때마다 이 표를 고친다.

## 다음 단계 진행 방법

이미 [0]·[1]은 끝났다. **단계마다 아래 3줄만 실행한다.** 예: 2단계(누적 10만)

```bash
cd scripts/seed/order
python generate.py --target 100000
python load.py out/stage-0100000
python verify.py --target 100000
```

- 출력이 깨지면(Windows 콘솔 한글) `set PYTHONIOENCODING=utf-8`(cmd) / `$env:PYTHONIOENCODING="utf-8"`(PowerShell) / `export PYTHONIOENCODING=utf-8`(Git Bash)
- 단계는 **순서대로만** 올린다. 건너뛰어도 동작은 하지만(목표 − 현재만 만든다) 단계별 측정 비교가 끊긴다
- `verify.py`가 실패하면 **다음 단계로 가지 않는다.** 원인을 확인하고 필요하면 `python rollback.py out/stage-0100000`으로 되돌린다
- 사람이 표로 직접 확인하려면: `docker exec -i mysql mysql -uroot -p1234 --default-character-set=utf8mb4 -t festival2 < verify-queries.sql`

### 단계를 끝낸 뒤 남길 것 (policy 3.3)

1. 이슈 #18 코멘트 — 아래 형식
2. [ADR-0004](../../../docs/adr/ADR-0004-order-bulk-load-with-csv-load-data.md) "개선 실측치" 표의 해당 단계 행
3. 위 "단계 진행 현황" 표
4. 커밋 — `🌱 chore(order): 주문 누적 N건 적재 (단계 k)` + `Refs: #18` (`out/`은 커밋하지 않는다)

```markdown
### 단계 k — 누적 N건 적재 결과
- 생성: 주문 a / 항목 b / 단위 c (seed s, 생성 x초)
- 적재: LOAD DATA 합계 y초 (묶음 m개, load-report.json)
- id 구간: orders … / order_items … / order_item_units …
- 검증: n/n 통과 (verify.py 출력 첨부)
```

## AI에게 맡길 때

이 저장소의 에이전트는 `AGENTS.md`를 따르므로, **이슈·ADR·LLD가 이미 있는 작업이라는 것**과 **어느 단계까지 할지**만 정확히 주면 된다.
아래를 그대로 붙여 넣고 `N`만 바꾼다.

```text
이슈 #18 주문 대량 적재의 다음 단계를 진행해줘. 목표는 누적 N건이야.
- 근거: ADR-0004(승인), LLD-0006(확정). 새 이슈·ADR·LLD는 만들지 마.
- 절차: scripts/seed/order/README.md "다음 단계 진행 방법" 그대로 (generate → load → verify).
- verify.py가 하나라도 실패하면 멈추고 원인과 선택지를 보고해. 임의로 롤백하거나 기준을 바꾸지 마.
- 통과하면 README "단계를 끝낸 뒤 남길 것" 1~4를 해줘 (이슈 코멘트, ADR-0004 결과 표, README 현황 표, 커밋).
- push와 PR은 내가 따로 말할 때만.
```

AI가 지켜야 할 것 (요청 문구에 이미 들어 있지만, 결과를 검토할 때 확인한다):

- 생성 전에 `docker info`가 실패하면 Docker를 직접 켜지 말고 사용자에게 요청한다 (AGENTS.md 6.0)
- 적재 중 주문 API를 호출하지 않는다 — V17(`idempotency`·`outbox` 불변)이 깨진다
- `fixtures/`를 다시 만들지 않는다 (`prepare.py --force` 금지) — 대표 메뉴·축제 날짜·기준값이 바뀌어 이전 단계와 비교할 수 없다
- 기준(허용 오차 등)을 바꿔야 하면 LLD-0006을 먼저 고치고 사용자 확인을 받는다

## 처음부터 다시 해야 할 때 (DB를 초기화한 경우 등)

`fixtures/baseline.json`은 **적재 전 DB 상태**(기존 주문 id 상한 2,828, 건수 2,284 등)를 전제로 한다.
DB 볼륨을 지웠다면 기존 서경대 주문을 먼저 복원한 뒤 [1]부터 다시 하고, 기존 주문이 달라졌다면 `prepare.py --force`로 fixture를 다시 만들고 그 사실을 LLD-0006에 남긴다.

## 실패했을 때

| 상황 | 조치 |
|---|---|
| `load.py`가 묶음 k에서 멈춤 | `python rollback.py out/stage-000N --chunk k` → `python load.py out/stage-000N --from-chunk k` |
| 단계 전체를 되돌림 | `python rollback.py out/stage-000N` (가장 최근 단계부터만) |
| 생성 결과를 다시 만들고 싶음 | `python generate.py --target N --force` (DB 상태가 같으면 같은 파일) |

## 파일

| 파일 | 역할 |
|---|---|
| `common.py` | DB 호출(`docker exec`), 최대 잉여 배분, 물 채우기 배분, fixture 로드 |
| `prepare.py` | 학교 그룹·부스·메뉴(대표 여부)·인기 부스·축제 날짜·기준값 fixture 생성, 부스 OPEN |
| `load_operations.py` | 축제 일정 적재 (`LOAD DATA ... IGNORE` — 서경대 기존 일정은 건너뜀) |
| `generate.py` | 누적 목표 − 현재 건수만큼 주문 생성 |
| `load.py` | 묶음 적재 + 건수 확인, 이미 들어간 묶음은 건너뜀 |
| `verify.py` | 분포·정합성 검증 18종 |
| `rollback.py` | id 구간 삭제 (자식 테이블부터 5만 행씩) |
| `fixtures/` | **커밋한다** — 재현의 기준 (다시 만들면 대표 메뉴·축제 날짜가 바뀔 수 있다) |
| `out/` | 커밋하지 않는다 — 500만 단계는 수 GB |

## 주의

- `BoothScheduler`가 매일 01:00 모든 부스를 `CLOSED`로 되돌린다. 적재에는 영향이 없다 (부스 상태를 보지 않음)
- 적재 중 앱 3대가 떠 있어도 된다. 다만 이 기간에 주문 API를 호출하면 그 주문이 분포를 깨고 `verify.py`의 V17(`idempotency`·`outbox` 불변)이 실패한다
- `idempotency`·`outbox`에는 아무것도 넣지 않는다 (policy 3.4)
