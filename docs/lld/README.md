# LLD (Low-Level Design)

특정 기능의 **구현 직전 상세 설계**를 기록한다. "이 문서만 보고 구현할 수 있는 수준"이 기준이며,
특히 **13장 테스트 계획이 TDD Red 단계의 직접적인 입력**이다.

새 LLD는 [`../templates/lld.md`](../templates/lld.md)를 복사해 `LLD-NNNN-<kebab-slug>.md`로 만든다. 번호는 기존 최대 번호 + 1 (4자리).

> **작성 후 아래 표에 반드시 한 줄을 추가한다.** (AGENTS.md 워크플로 ③)
> 구현이 끝나면 상태를 `구현 완료`로, 설계가 바뀌면 **문서를 먼저 고친 뒤** 코드를 수정한다.

## 목록

| 번호 | 제목 | 상태 | 작성일 | 이슈 | 근거 ADR | 작업 갈래 |
| --- | --- | --- | --- | --- | --- | --- |
| [LLD-0001](LLD-0001-university-department-and-auth-identity.md) | University·Department 도메인 신설과 인증 식별자 전환 | 구현 완료 | 2026-09-14 | [#3](https://github.com/silversieon/likelion14th-festival-be-V2/issues/3) | [ADR-0001](../adr/ADR-0001-university-department-schema-and-auth-identity.md) | ① 전국 확장 |
| [LLD-0002](LLD-0002-university-department-search-api.md) | 로그인 화면용 학교명 검색·학과 목록 조회 API | 구현 완료 | 2026-09-16 | [#5](https://github.com/silversieon/likelion14th-festival-be-V2/issues/5) | 해당 없음 (사용자 지시 기반) | ① 전국 확장 (후속) |
| [LLD-0003](LLD-0003-booth-menu-bulk-seed.md) | 전국 학과별 부스·번역·메뉴 대량 적재 (Flyway V17~V19) | 구현 완료 | 2026-09-17 | [#12](https://github.com/silversieon/likelion14th-festival-be-V2/issues/12) | [ADR-0002](../adr/ADR-0002-booth-menu-bulk-seed-path.md) | ② 대량 데이터 삽입 |

<!--
  행 예시 (첫 LLD 작성 시 위 안내 행을 지우고 이 형식으로 채운다):
  | [LLD-0001](LLD-0001-university-booth-mapping.md) | 대학-부스 연관관계 스키마 및 조회 API 개편 | 구현 완료 | 2026-09-15 | #1 | [ADR-0001](../adr/ADR-0001-university-booth-schema.md) | ① 전국 확장 |
-->

## 상태의 의미

| 상태 | 뜻 |
|---|---|
| 작성 중 | 설계가 확정되지 않았다. **구현을 시작하지 않는다** |
| 확정 | 설계 확정. TDD 사이클(Red)의 입력으로 쓸 수 있다 |
| 구현 완료 | 13장 테스트 계획의 모든 항목이 Green + Refactor까지 끝났고 `./gradlew test`가 통과했다 |
| 폐기 | 설계가 무효화되었다 (사유와 대체 문서를 본문에 남긴다) |

## 이 프로젝트에서 LLD가 특히 신경 써야 하는 것

작업 갈래(AGENTS.md 1.4)마다 반드시 채워야 하는 장이 다르다. **해당 없는 장은 지우지 말고 "해당 없음"이라고 적는다** — 지우면 "빠뜨린 것"과 "해당 없는 것"이 구분되지 않는다.

| 작업 갈래 | 반드시 채우는 장 |
|---|---|
| ① 전국 대학 확장 | 2(데이터 모델), 7(스키마 변경 + **백필**), 8(인덱스), 12(에러), **13.3(분산 검증 — 마이그레이션 동시 기동)** |
| ② 대량 데이터 삽입 | **9(데이터 생성 스펙)**, 7(스키마), 13.4(측정) |
| ③ 성능 개선 | **8(인덱스/쿼리 + 실행 계획 비교)**, 10(캐싱), **13.4(성능 측정)** |
| ④ Read Replica·CQRS | 3.4(조회 전용 서비스), **10(Read Model·정합성 수준)**, 11(트랜잭션), **13.3(분산 검증)** |
| SSE·이벤트·스케줄러·멱등성·Redis 변경 | **13.3(분산 환경 검증) — 3인스턴스로 확인하지 않으면 완료가 아니다** (policy 17장) |

## 작성 시 자주 놓치는 것

- **승인되지 않은 ADR("제안됨" 상태)을 근거로 LLD를 쓰는 것** — ADR은 개발자 승인(AGENTS.md ②-1)을 받은 뒤에야 구현 근거가 된다
- **근거 ADR이 없으면** "해당 없음 (사용자 지시 기반)"과 **지시 요약**을 반드시 남긴다 (AGENTS.md ③)
- **대량 데이터의 규모·편향·분포는 사용자 프롬프트를 그대로 인용한다.** 에이전트가 임의로 가정하면 그 LLD는 무효다 (policy 3장)
- **컨트롤러가 있는 작업**은 `docs/api-spec/<domain>.md`를 **먼저** 작성·갱신하고, 6장에서는 그 문서를 참조만 한다 (중복 기술 금지)
- **스키마를 바꾸면** Flyway 마이그레이션 파일과 `docs/erd/`를 같은 PR에서 함께 고친다. 7.3 체크박스를 비워 두지 않는다
- **인덱스를 추가하는데 `EXPLAIN` Before/After가 없는 것** — 실측 없는 인덱스 추가는 policy 5장 위반이다
- **주문 상태를 바꾸는 기능인데 SSE 이벤트를 2.4에 안 적는 것** — 이벤트 이름은 프런트와의 계약이다 (api-conventions 8장)
- **행 증폭을 무시하는 것** — 1 order → N order_items → Σquantity order_item_units (ERD 3.4)
- 14장 미해결 질문은 **다음 작업의 입력**이다. 해결되면 취소선 + 해결 근거(ADR·이슈 번호)를 남긴다

## 관련 문서

- 방침: [`../policy/development-policy.md`](../policy/development-policy.md)
- 결정 기록: [`../adr/README.md`](../adr/README.md)
- API 규약: [`../api-spec/api-conventions.md`](../api-spec/api-conventions.md)
- ERD: [`../erd/erd-0001-initial-schema.md`](../erd/erd-0001-initial-schema.md)
- 템플릿: [`../templates/lld.md`](../templates/lld.md)
- TDD 스킬: `.agents/skills/tdd-red|tdd-green|tdd-refactor/SKILL.md`
- 사본: 모든 LLD는 스터디 저장소 `C:\code\study\festival\lld\`에도 동기화한다 (AGENTS.md 워크플로 ⑥)
