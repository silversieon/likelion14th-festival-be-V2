# AGENTS.md — 축제 서비스 에이전트 작업 가이드

이 문서는 코딩 에이전트가 본 프로젝트에서 작업할 때 반드시 따라야 하는 규칙과 워크플로를 정의한다.
**어떤 개발 작업이든 이 문서의 워크플로를 벗어나서 진행하지 않는다.**

> **필독**: 이 문서를 읽은 뒤 **반드시 `docs/policy/development-policy.md`를 읽어야 한다.**
> 도메인 방침(대용량 데이터 처리, 인덱싱/쿼리 튜닝, 캐싱, CQRS 조회, Read Replica, 멱등성, 인증, 로깅, 에러 정책)은
> 모두 그 문서에 정의되어 있으며, 모든 ADR·LLD·구현은 해당 방침을 전제로 한다.

## 1. 프로젝트 개요

- **목적**: 대학 축제 서비스 (축제/부스 소개, QR 오더 기반 주문, 분실물, 운영자 관리)를 **전국 단위 대학 규모**로 확장하고, 대규모 트래픽·데이터 환경에서도 견디는 구조로 고도화한다.
- **저장소**: `silversieon/likelion14th-festival-be-V2`
- **기본 브랜치**: **`develop`** (이 저장소에는 `main` 브랜치가 없다. 모든 작업 브랜치는 `develop`에서 분기하고 `develop`으로 PR한다.)
- **베이스 패키지**: `com.skulikelion.festival`

### 1.1 기술 스택 (build.gradle 기준 — 실제 버전)

| 구분 | 사용 기술 |
|---|---|
| 언어 / 빌드 | Java 21 (toolchain), Gradle, **Spring Boot 4.0.5**, dependency-management 1.1.7 |
| 웹 | spring-boot-starter-web, -validation |
| 영속성 | spring-boot-starter-data-jpa (Hibernate), MySQL 8, **Flyway** (`spring-boot-starter-flyway` + `flyway-mysql`) |
| 캐시/메시징 | spring-boot-starter-data-redis (Redis Pub/Sub, **Redis Streams**) |
| 인증 | Spring Security, JJWT 0.12.5 (Access/Refresh) |
| 직렬화 | **Jackson 3.x — 패키지가 `tools.jackson.*`이다** (`com.fasterxml.jackson.*` 아님. 혼용 주의) |
| 문서화 | springdoc-openapi-starter-webmvc-ui 2.8.1 |
| 스토리지 | AWS SDK v2 (S3), MinIO 8.4.3, scrimage(webp), metadata-extractor |
| 모니터링 | Actuator, micrometer-registry-prometheus |
| 복원력 | Resilience4j 2.4.0 (spring-boot4) |
| 테스트 | JUnit5, spring-boot-starter-test, spring-security-test, **Testcontainers 2.0.5** (MySQL + Redis) |
| 코드 스타일 | **spotless + googleJavaFormat** — `compileJava`가 `spotlessApply`에 의존하므로 **빌드만 해도 소스가 자동 포맷된다** (2-space 들여쓰기, import 정렬 `java/javax/jakarta/org/com`, 미사용 import 제거, 라이선스 헤더 자동 삽입) |

- **미도입**: QueryDSL, MongoDB, Kafka. 조회는 Spring Data JPA + `@Query`(JPQL/native)로 작성한다.
- **향후 추가 예정**: MongoDB(Read Model / 비정규화 조회 전용), 이 외 대량 데이터·조회 성능 이슈 해결 과정에서 필요한 기술은 추가로 도입 가능 (도입 시 ADR 필수).

### 1.2 설계 방법론

현재는 **도메인형 패키지 구조**를 유지한다. DDD·레이어드 아키텍처(4계층, 의존성 역전 등)는 이번 범위에서 적용하지 않으며, 추후 별도 ADR을 거쳐 도입 여부를 결정한다.
지금 이 문서는 그 이전 단계 — **대규모화·성능·조회 구조 개선**에 초점을 맞춘다.

### 1.3 현재까지 구현된 기반 (작업 시작 전 반드시 인지)

새 설계는 아래 기존 구조 **위에** 얹는다. 같은 문제를 다시 푸는 설계를 제안하지 않는다.

| 기반 | 위치 | 요약 |
|---|---|---|
| 공통 응답 | `global/common/BaseResponse` | `{success, code, message, data}` 4필드 envelope. 상세는 `docs/api-spec/api-conventions.md` |
| 전역 예외 처리 | `global/exception/GlobalExceptionHandler` | `CustomException(BaseErrorCode)` → `BaseResponse.error(status, message)` |
| 인증 | `global/security`, `global/filter/JwtAuthenticationFilter` | JWT. Access는 `Authorization: Bearer` **또는 `ACCESS_TOKEN` 쿠키**, Refresh는 `REFRESH_TOKEN` 쿠키. 토큰 subject = `Department` 이름 |
| 추적 ID | `global/filter/MdcFilter` | 요청마다 8자리 `traceId`와 `clientIp`를 MDC에 적재 (**로그 전용 — 응답 본문·헤더로 내려주지 않는다**) |
| 커서 페이지네이션 | `global/common/pagenation` | `CursorCodec`(Base64 인코딩) + `CursorPageResponse{items, nextCursor, hasNext, size}` |
| 멱등성 | `global/util/idempotency` | `@Idempotent(idempotencyKey = "#key", strategy = ...)` AOP. 전략 4종: `DB`, `REDIS`, `WRITETHROUGH`, `FALLBACK`. 현재 주문 생성은 `FALLBACK` |
| 트랜잭셔널 아웃박스 | `global/outbox` | `Outbox` 엔티티 + `PollingPublisher`(`@Scheduled(fixedDelay=200)`, `FOR UPDATE SKIP LOCKED`로 폴링) |
| 주문 이벤트 발행 | `domain/order/event` | `order.event.pattern` = `direct`(기본, 트랜잭션 내 직접 전파) \| `outbox`(아웃박스 경유) |
| SSE 알림 | `domain/order/sse` | `sse.strategy` = `local` \| `distributed`. distributed일 때 `sse.redis.mode` = `pubsub` \| **`stream`**(Redis Streams — 소비자 그룹 + ACK로 유실 방지) |
| 스키마 이력 | `src/main/resources/db/migration` | Flyway `V1`~`V13`. 현재 스키마는 `docs/erd/erd-0001-initial-schema.md` 참조 |
| 성능 계측 | `global/util/timetrace` | `@TimeTrace` AOP — 메서드 실행 시간 로깅 |

> 설정 값(`application*.yml`)은 에이전트가 직접 열지 않는다. 토글의 **존재와 허용 값**은 위 표와 `global/config/property/*`의 `@ConfigurationProperties` 레코드에서 확인한다.

### 1.4 이번 프로젝트의 핵심 작업 범위

이 문서에서 다루는 에이전트 작업은 다음 네 갈래로 진행된다. ADR/LLD를 작성할 때는 어느 항목에 해당하는 작업인지 컨텍스트에 명시한다.

1. **전국 단위 대학으로 서비스 확장**
    - 기존에는 우리 학교 내부 학과(부스)만 존재했으나(`global/enums/Department` 하드코딩 enum), 여러 대학이 추가되고 **대학 ↔ 부스 간 연관 관계**를 매핑하는 등 데이터베이스 스키마 개편이 필요함
    - 스키마 변경은 Flyway 마이그레이션으로 관리하며, 변경 전 ERD 문서를 갱신한다
2. **대량 데이터 삽입**
    - 개념 스키마 세팅 완료 후 수십만 → 수백만 → 수천만 건 규모로 단계적으로 데이터 적재
    - 배치 삽입(batch insert) vs 프로시저 방식은 상황에 따라 ADR로 결정
    - 데이터 편향·분포는 매 작업마다 사용자(프롬프트)가 직접 지정 — 에이전트가 임의로 분포를 가정하지 않는다
3. **대량 데이터 기준 API 성능 문제 파악 및 해결**
    - 기존 조회 API·삽입 API를 대상으로 병목 원인을 실측 기반으로 파악
    - 인덱스 설계/적용, 쿼리 튜닝, 캐싱(Redis) 등 가용한 기법을 상황에 맞게 적용
    - 현재 스키마에는 **FK 인덱스와 소수의 UNIQUE 외에 조회용 인덱스가 사실상 없다** (ERD 문서 6장 참조). 이것이 3번 작업의 출발점이다
4. **Read Replica / CQRS 도입**
    - Read Replica 구성, CQRS 패턴 적용에 따른 Read Model 설계
    - 조회 전용 저장소로 MongoDB 활용 (비정규화된 조회 모델)

### 1.5 문서 체계 — 내용을 어디에 쓰는가

| 쓰려는 내용 | 위치 |
|---|---|
| 프로젝트 전체에 지속 적용되는 방침 (대용량 처리, 인덱싱/캐싱 원칙, CQRS 조회 원칙 등) | `docs/policy/development-policy.md` |
| 특정 시점의 기술/아키텍처 결정과 근거 (옵션 비교 포함) | `docs/adr/ADR-NNNN-*.md` |
| 특정 기능의 구현 직전 상세 설계 | `docs/lld/LLD-NNNN-*.md` |
| REST API 계약 | `docs/api-spec/<domain>.md` (공통 규약은 `api-conventions.md`) |
| 현재 DB 스키마 전체 상 | `docs/erd/erd-NNNN-*.md` |
| 에이전트 작업 절차·워크플로·아키텍처 구조 규칙 | 본 문서 (`AGENTS.md`) |

- policy와 ADR이 충돌하면 **최신 ADR이 우선**하며, 해당 ADR은 policy 갱신을 동반해야 한다.
- policy 변경은 반드시 ADR을 거친다. (policy 절차 장 참조)

## 2. 개발 워크플로 (필수)

모든 작업은 아래 순서를 따른다. 단계를 건너뛰지 않는다.
**모든 단계는 `docs/policy/development-policy.md`의 방침을 전제로 한다.**

```mermaid
flowchart TD
    A[사용자 요구사항 접수] --> B["① 이슈 생성<br/>.agents/skills/issue/SKILL.md 준수<br/>.github/ISSUE_TEMPLATE 양식 사용"]
    B --> C{아키텍처/기술<br/>결정이 필요한가?<br/>(스키마 개편, 배치/프로시저 선택,<br/>인덱스·캐싱 전략, CQRS/Read Replica/MongoDB 도입 등)}
    C -- "예" --> D["② ADR 작성<br/>docs/templates/adr.md 템플릿 사용<br/>→ docs/adr/ADR-NNNN-제목.md<br/>고려한 옵션 2~3개 비교"]
    C -- 아니오 --> E
    D --> DG{{"⛔ ②-1 개발자 확인 — 필수 중단점<br/>작성 완료 보고 + 옵션 요약<br/>+ 권고안 + 열린 질문<br/>여기서 멈추고 응답을 기다린다"}}
    DG -- "수정·재검토 요청" --> D
    DG -- "옵션 선택 + 추가 지시" --> E
    E["③ LLD 작성<br/>docs/templates/lld.md 템플릿 사용<br/>→ docs/lld/LLD-NNNN-제목.md<br/>(승인된 ADR 또는 사용자 프롬프트 지시 기반)<br/>※ 테스트 계획 장이 TDD Red의 입력"]
    E --> F1["④ 구현 — TDD Red<br/>.agents/skills/tdd-red/SKILL.md<br/>실패하는 단위 테스트 작성<br/>(컴파일 O, assertion 실패)"]
    F1 --> F2["④ 구현 — TDD Green<br/>.agents/skills/tdd-green/SKILL.md<br/>테스트를 통과시키는 최소 구현<br/>(테스트 코드 수정 금지)"]
    F2 --> F3["④ 구현 — TDD Refactor<br/>.agents/skills/tdd-refactor/SKILL.md<br/>테스트를 안전망 삼아 구조 개선<br/>(외부 동작 변경 금지)"]
    F3 --> F4{LLD 테스트 계획의<br/>다음 동작이 남았는가?}
    F4 -- "예 (다음 사이클)" --> F1
    F4 -- 아니오 --> G["⑤ 커밋<br/>.agents/skills/commit/SKILL.md 준수<br/>gitmoji + Conventional Commits + Refs: #이슈번호"]
    G --> H{완료 조건<br/>DoD 충족?}
    H -- 아니오 --> F1
    H -- 예 --> S["⑥ ADR·LLD 스터디 저장소 동기화<br/>C:/code/study/festival/adr, lld 로 복사<br/>해당 경로에서 add·commit·push (main 직접)"]
    S --> I["⑦ PR 생성 (base: develop)<br/>.agents/skills/pr/SKILL.md 준수<br/>.github/pull_request_template.md 양식<br/>Closes #이슈번호 + ADR/LLD 링크"]
```

### 문서 작성 스타일 (ADR·LLD 공통, 필수)

**작성자(사용자)는 대규모 트래픽/데이터 환경에서의 성능 최적화, CQRS, Read Replica를 캡스톤 과정에서 처음 실전 적용해본다.** 따라서 ADR·LLD를 작성할 때:

- 새로운 개념이 **처음 등장하는 지점마다 괄호로 쉬운 설명을 병기**한다.
    - 예: "커버링 인덱스(조회에 필요한 컬럼을 모두 인덱스에 포함시켜, 실제 테이블을 다시 읽지 않고 인덱스만으로 조회를 끝내는 방식)"
    - 예: "Read Replica(쓰기는 원본 DB에, 읽기는 복제본 DB로 분산시켜 조회 부하를 나누는 구성)"
    - 예: "CQRS(Command와 Query를 분리하는 패턴 — 쓰기 모델과 읽기 모델을 서로 다른 구조·저장소로 둘 수 있음)"
    - 예: "배치 삽입(insert를 한 건씩 보내지 않고 여러 건을 묶어 한 번에 보내 왕복 횟수를 줄이는 방식)"
- 가능하면 **기존 방식(단일 MySQL, `domain/<도메인>` 패키지 + controller/service/repository, 동기 JPA 조회)과 무엇이 달라지는지** 한 줄 비교를 덧붙인다.
- 설계 결정에는 "왜 이렇게 하는지"(실측된 병목이 무엇이었는지, 어떤 지표가 개선 근거인지)를 반드시 한 줄 이상 남긴다. **대규모 데이터/성능 작업의 특성상, 가능하면 Before/After 수치(응답시간, TPS, 쿼리 실행 계획 등)를 근거로 남긴다.**
- 이 규칙은 문서를 캡스톤 발표 자료·학습 자료로도 활용하기 위함이다 (⑥ 스터디 저장소 동기화 참조).

### 깃모지(gitmoji) 컨벤션 (커밋·이슈·PR 공통, 필수)

**커밋 메시지 subject, 이슈 제목, PR 제목의 맨 앞에는 상황에 맞는 깃모지를 붙인다.**

- type별 기본 깃모지: ✨ feat / 🐛 fix / ♻️ refactor / 📝 docs / ✅ test / 🔧 chore / 🎨 style / ⚡️ perf
- 대용량 데이터·성능 작업에서 자주 쓰이는 깃모지: 🗃️ DB 스키마 변경, ⚡️ 성능 개선(인덱스/쿼리 튜닝/캐싱), 🌱 시드/더미 데이터 스크립트, 🔀 CQRS·Read Model 관련 구조 변경
- 더 구체적인 상황의 전체 표는 `.agents/skills/commit/SKILL.md`를 따른다.
- **scope는 이 프로젝트의 도메인·영역 이름을 쓴다**: `auth`, `booth`, `menu`, `lostitem`, `manager`, `order`, `sse`, `outbox`, `idempotency`, `global` / 코드 외: `build`, `ci`, `docs`, `adr`, `lld`, `erd`, `policy`
  (전국 확장 작업이 시작되면 `university`가 추가된다)
- 형식 요약:
    - 커밋: `⚡️ perf(booth): 부스 목록 조회 커버링 인덱스 적용`
    - 이슈: `🗃️ [Feature] university: 대학-부스 연관관계 스키마 개편`
    - PR: `🔀 refactor(order): 주문 조회 Read Model(MongoDB) 분리 (#23)`

> **주의**: 저장소에 `.github/labels.json`은 존재하지 않는다. 라벨은 GitHub에 이미 등록된 것을 `gh label list`로 확인해 사용하고, 없으면 새로 만들기 전에 사용자에게 확인한다.

### ① 이슈 생성 (개발 전, 항상 최초)

- 코드를 작성하기 **전에** `.agents/skills/issue/SKILL.md`에 따라 GitHub 이슈를 생성한다.
- 본문 양식은 `.github/ISSUE_TEMPLATE/` 하위의 마크다운 템플릿을 사용한다. 실제 파일은 다음 4종이다:
  `✨-feature.md`, `🐛-fix.md`, `♻️-refactor.md`, `📝-documentation.md`
- 생성된 이슈 번호는 이후 모든 산출물(브랜치, ADR, LLD, 커밋, PR)에서 참조한다.
- 이슈 생성 직후 **`develop`에서** 작업 브랜치를 분기한다: `<type>/<이슈번호>-<영문-kebab-설명>`
  (기존 브랜치 예: `refactor/idempotency-redis`)

### ② ADR 작성 (아키텍처/기술 결정 시)

- 다음에 해당하면 **구현 전에 반드시** ADR을 작성한다:
    - 스키마 개편 (대학-부스 연관관계, 정규화/비정규화 수준 결정 등)
    - 대량 데이터 삽입 방식 결정 (배치 insert vs 프로시저, 트랜잭션 크기 등)
    - 성능 개선 기법 선택 (인덱스 전략, 캐싱 전략, 페이징 전략 등)
    - 저장소·아키텍처 구조 변경 (Read Replica 도입, CQRS 적용, MongoDB 등 신규 저장소 도입)
- `docs/templates/adr.md` 템플릿을 복사하여 `docs/adr/ADR-NNNN-<kebab-제목>.md`로 작성한다.
  번호는 기존 ADR 최대 번호 + 1 (4자리, 0001부터).
- 템플릿의 모든 섹션(컨텍스트, 결정 동인, 고려한 옵션, 결정, 결과)을 채운다. **성능·데이터 규모 관련 결정은 실측 수치를 컨텍스트/결과 섹션에 포함한다.**
- **고려한 옵션은 2~3개를 비교한다.**
    - **2개**: 선택지가 명확히 둘로 갈릴 때
    - **3개**: 절충안이나 단계적 도입안(예: ① 현상 유지 ② 인덱스만 추가 ③ 스키마 비정규화)이 **실제로 성립할 때만** 세 번째로 넣는다
    - 4개 이상으로 늘리지 않는다. 개발자가 고르기 어려워지고 비교의 초점이 흐려진다.
    - **실제로 검토하지 않은 허수 옵션을 채우지 않는다.** 2개로 충분하면 2개만 쓴다. "아무것도 하지 않기"도 정당한 옵션이다.
- **문서 작성 스타일 규칙(위)을 적용한다** — 새로운 개념에 괄호 설명 병기.
- **작성 후 `docs/adr/README.md`의 목록 표에 한 줄을 추가한다.** 기존 ADR의 상태가 바뀌면(대체됨·폐기됨) 그 행의 상태 칸도 함께 고친다. 인덱스가 없는 ADR은 없는 것과 같다.
- ADR은 `docs(adr)` 커밋으로 구현 코드와 분리하여 커밋한다. **단, 커밋도 ②-1 승인 이후에 한다.**
- 기존 결정 범위 내의 단순 작업이면 ADR을 생략할 수 있으나, 이슈에 사유를 명시한다.

### ②-1 개발자 확인 (ADR 작성 직후 — ⛔ 필수 중단점)

**ADR을 작성했으면 거기서 멈춘다. 이어서 다음 단계로 넘어가지 않는다.**
아키텍처 결정은 개발자가 내리는 것이고, 에이전트는 선택지를 정리해 제시하는 역할이다.

**에이전트가 개발자에게 전달할 것 (네 가지 모두):**

1. **작성 완료 보고** — ADR 번호·제목·파일 경로
2. **옵션 요약** — 각 옵션을 2~3줄로. 무엇을 얻고 **무엇을 포기하는지**를 반드시 함께 적는다
3. **권고안과 근거** — 어떤 옵션을 왜 권하는지. 개발자가 그대로 따를 필요는 없다는 점을 명확히 한다
4. **판단이 필요한 열린 질문** — 에이전트가 정할 수 없는 것
   (예: 허용 지연 시간, 데이터 보관 기간, 축제 기간 중 마이그레이션 가능 여부, 프런트 계약 변경 가능 여부)

**승인 전에 하지 않는 것:**

- ❌ LLD 작성 (③)
- ❌ 구현 코드·Flyway 마이그레이션 파일 작성 (④)
- ❌ 커밋(⑤)·스터디 저장소 동기화(⑥)·PR 생성(⑦)
- ❌ ADR 상태를 "승인됨"으로 바꾸기 — **에이전트가 스스로 승인하지 않는다**

**개발자 응답을 받은 뒤:**

- 응답 내용을 ADR의 **"개발자 응답 기록"** 표에 남긴다. **추가 지시 사항은 원문 그대로** 적는다 (요약·의역 금지).
- 헤더 표의 `승인` 칸과 `상태`(→ 승인됨), "결정" 절의 **확정** 줄을 갱신한다.
- 개발자가 **권고와 다른 옵션**을 골랐으면 "결정" 절을 그 옵션으로 고쳐 쓰고, 권고안은 이력으로 남긴다.
- 개발자가 **추가 조건·제약을 붙였으면** "구현 규칙"에 반영한 뒤 ③으로 넘어간다.
- 개발자 지시가 이 ADR의 전제를 뒤집으면 **ADR을 고쳐 다시 ②-1로 돌아간다.** 어긋난 채로 LLD를 쓰지 않는다.
- 개발자가 "그대로 진행"이라고만 답해도, **어떤 옵션으로 확정되었는지는 반드시 명시적으로 확인**하고 기록한다.

> ADR이 필요 없는 작업(C에서 "아니오")은 이 단계를 거치지 않고 ③으로 간다.

### ③ LLD 작성 (구현 시작 전, 항상)

- **구현을 시작하기 전에 반드시** LLD(Low-Level Design)를 작성한다.
- 설계의 근거는 다음 우선순위를 따른다:
    1. **②-1에서 승인된** ADR 또는 기존의 승인된 ADR. **승인되지 않은 ADR("제안됨" 상태)을 근거로 LLD를 쓰지 않는다.**
    2. ADR이 없는 경우 → **사용자의 프롬프트 지시**를 근거로 작성하고, 문서의 "관련 ADR" 항목에 "해당 없음 (사용자 지시 기반)"과 지시 요약을 남긴다. **대량 데이터의 편향·분포 조건은 항상 사용자 프롬프트 지시를 그대로 인용한다 — 에이전트가 임의로 가정하지 않는다.**
- `docs/templates/lld.md` 템플릿을 복사하여 `docs/lld/LLD-NNNN-<kebab-제목>.md`로 작성한다.
  번호는 기존 LLD 최대 번호 + 1 (4자리, 0001부터).
- 대상 엔티티/테이블 구조, API 명세, 인덱스/쿼리 변경안, 데이터 생성 스펙(규모·분포·삽입 방식), 캐싱 키 설계, 테스트 계획까지
  **이 문서만 보고 구현할 수 있는 수준**으로 작성한다.
- 특히 **테스트 계획**은 다음 단계 TDD Red의 직접적인 입력이므로, 검증할 동작 단위로 구체적으로 작성한다. 성능 관련 작업은 기능 테스트 외에 **측정 방법(부하 테스트 시나리오, 측정 지표)**도 함께 명시한다.
- **API 스펙 선행 작성 (컨트롤러가 있는 작업 필수)**: REST API를 추가·변경하는 작업은 LLD 작성 시점에
  `docs/api-spec/<domain>.md`를 `docs/api-spec/api-conventions.md`의 공통 규약·템플릿에 맞게 먼저 작성/갱신한다.
    - LLD의 API 명세 장은 상세를 중복 기술하지 않고 해당 스펙 문서를 참조한다.
    - **컨트롤러 구현은 스펙 문서와 일치해야 하며**, 다르게 구현해야 하면 스펙 문서를 먼저 수정한다. 스펙 갱신은 구현과 같은 PR에 포함한다.
    - 공통 응답 형식(`BaseResponse`)·에러 응답·헤더 규약은 api-conventions.md가 단일 기준이다 (개별 스펙에서 재정의 금지).
- **스키마를 바꾸는 작업은 `docs/erd/`의 최신 ERD 문서를 함께 갱신한다.** Flyway 마이그레이션 파일과 ERD 문서가 어긋난 상태로 커밋하지 않는다.
- **문서 작성 스타일 규칙(위)을 적용한다** — 새로운 개념에 괄호 설명 병기, 기존 방식과의 비교 첨부.
- **작성 후 `docs/lld/README.md`의 목록 표에 한 줄을 추가한다.** 구현이 끝나면 그 행의 상태를 `구현 완료`로 바꾼다.
- LLD는 `docs(lld)` 커밋으로 남기고, 구현 중 설계가 바뀌면 문서를 갱신한 뒤 코드를 수정한다.

### ④ 구현 (TDD — Red → Green → Refactor)

**구현은 반드시 TDD 사이클로 진행한다.** LLD의 테스트 계획에 나열된 동작 하나하나가 사이클의 단위이며,
각 단계는 `.agents/skills` 하위의 해당 스킬 문서를 따른다.

| 단계 | 스킬 문서 | 핵심 규칙 |
|---|---|---|
| **Red** | `.agents/skills/tdd-red/SKILL.md` | LLD의 테스트 계획·시그니처를 입력으로 **실행되며 assertion에서 실패하는** 단위 테스트를 작성한다. 비즈니스 로직은 한 줄도 작성하지 않으며, 컴파일을 위한 최소 골격(skeleton)까지만 허용한다. |
| **Green** | `.agents/skills/tdd-green/SKILL.md` | 현재 테스트를 통과시키는 **최소 구현**만 작성한다. 테스트 코드는 수정하지 않는다. 과도한 일반화 금지. |
| **Refactor** | `.agents/skills/tdd-refactor/SKILL.md` | 테스트를 안전망 삼아 중복 제거·책임 분리 등 구조를 개선한다. 외부 동작 변경 금지, 매 변경 후 전체 테스트 통과 확인. |

- Red 단계의 사전 정보(패키지, 클래스/메서드 시그니처, DTO 필드, 비즈니스 규칙, 의존 객체)는
  **LLD 문서(데이터 모델, 시그니처 정의, 예외 정책, 테스트 계획)에서 가져온다.**
  LLD에 없는 정보가 필요하면 LLD를 먼저 보완한 뒤 진행한다.
- 대량 데이터 생성 스크립트·부하 테스트 스크립트처럼 운영 코드가 아닌 도구성 코드는 TDD 사이클을 강제하지 않되, LLD에 명시한 데이터 규모/분포 스펙을 반드시 반영하고 실행 결과(적재 건수, 소요 시간)를 커밋 메시지 또는 이슈에 기록한다.
- 사이클 순서는 하위 계층(repository) → 상위 계층(service) → controller 순으로, 안쪽에서 바깥쪽으로 진행한다.
- LLD 테스트 계획의 모든 항목이 Green + Refactor 완료될 때까지 사이클을 반복한다.
- 아래 3장 패키지 구조 규칙을 항상 준수한다.
- 구현 완료 기준: LLD 테스트 계획 전 항목 소화 + 이슈의 완료 조건(DoD) 충족 + `./gradlew test` 전체 통과.

### ⑤ 커밋 (개발 이후)

- 논리적 작업 단위마다 `.agents/skills/commit/SKILL.md`에 따라 커밋한다.
- TDD로 구현한 코드는 **하나의 Red→Green→Refactor 사이클(또는 응집된 몇 개의 사이클)이 완료된 시점**을 커밋 단위로 삼고, 테스트와 구현을 같은 커밋에 포함한다.
- 형식: `<gitmoji> type(scope): 한국어 요약` + footer `Refs: #이슈번호` (깃모지 표는 commit 스킬 참조)
- 테스트가 통과한 상태에서만 커밋한다.
- **`spotlessApply`가 `compileJava`에 묶여 있어 빌드/테스트만 돌려도 소스가 자동 포맷된다.** 커밋 직전 `git status`로 의도치 않은 포맷 변경이 섞였는지 확인하고, 섞였다면 같은 커밋에 포함시키되 커밋 본문에 그 사실을 남긴다.

### ⑥ ADR·LLD 스터디 저장소 동기화 (커밋 이후, PR 생성 전 — 필수)

이번 작업에서 작성·수정한 ADR과 LLD는 본 프로젝트 외에 **스터디 저장소에도 추가로 보관**한다.

- 대상 경로 (스터디 저장소 `C:\code\study` — 별도 GitHub 저장소, main 브랜치 사용):
    - `docs/adr/ADR-NNNN-*.md` → `C:\code\study\festival\adr\`
    - `docs/lld/LLD-NNNN-*.md` → `C:\code\study\festival\lld\`
    - (`festival` 디렉터리는 아직 없으므로 최초 동기화 시 생성한다. 기존 `banking` 디렉터리는 이전 프로젝트 산출물이며 건드리지 않는다.)
- 절차 (반드시 `C:\code\study` 경로에서 실행):
    1. `git -C C:\code\study pull` — 다른 기기에서 올린 내용과 먼저 동기화
    2. 이번 작업의 ADR/LLD 파일을 위 경로로 복사 (파일명 동일하게, 수정된 문서는 덮어쓰기)
    3. `git -C C:\code\study add festival/adr festival/lld`
    4. `git -C C:\code\study commit -m "📝 docs(festival): ADR-NNNN, LLD-NNNN 동기화"`
    5. `git -C C:\code\study push origin main`
- **기본 브랜치 직접 push는 스터디 저장소(`C:\code\study`)에서만 허용**된다.
  본 프로젝트(festival) 저장소는 브랜치 → PR 흐름을 사용하며, `develop` 직접 push는 사용자가 명시적으로 지시한 경우에만 한다.

### ⑦ PR 생성 (스터디 동기화 이후)

- 이슈의 DoD를 모두 충족하고 ⑥ 동기화를 마친 뒤 `.agents/skills/pr/SKILL.md`에 따라 PR을 생성한다.
- **base 브랜치는 `develop`이다.**
- 본문은 `.github/pull_request_template.md` 양식을 따르며(파일명 소문자 주의), `Closes #이슈번호`와 이번 작업의 ADR/LLD 문서 링크를 반드시 포함한다.
- PR을 열면 CI(`.github/workflows/ci.yml`)가 `spotlessCheck` → `build` 순으로 돈다. **`spotlessCheck` 실패는 포맷 문제이므로 로컬에서 `./gradlew spotlessApply` 후 다시 푸시한다.**

## 3. 패키지 구조 규칙 (도메인형 패키지, DDD 미적용)

### 3.1 현재 구조

**DDD·레이어드 아키텍처를 적용하지 않는다.** 최상위를 `domain`(비즈니스 도메인)과 `global`(횡단 관심사)로 나누고,
`domain` 아래를 도메인별로 다시 나누는 구조를 유지한다.

```
com.skulikelion.festival
├── domain/
│   ├── auth/            # 인증 (자체 엔티티 없음 — manager 도메인의 Manager를 사용)
│   │   ├── controller/  ├── dto/{request,response}/  ├── exception/  └── service/
│   ├── booth/           # 부스 + 메뉴 + 운영시간 + 다국어 번역
│   │   ├── controller/  ├── converter/  ├── dto/{request,response}/{booth,menu}/
│   │   ├── entity/      ├── enums/      ├── exception/  ├── mapper/
│   │   ├── repository/  ├── scheduler/
│   │   └── service/{booth,menu,dictionary,translation}/
│   ├── lostitem/        # 분실물
│   │   ├── controller/  ├── dto/  ├── entity/  ├── exception/
│   │   ├── mapper/      ├── repository/  └── service/
│   ├── manager/         # 부스/학생회 운영자 계정
│   │   └── (동일 구성 + entity/enums/)
│   └── order/           # 주문 — 이벤트·SSE가 붙는 가장 복잡한 도메인
│       ├── controller/  ├── dto/{request,response}/  ├── entity/{,enums}/
│       ├── event/{,listener,payload}/     # 주문 상태 변경 이벤트 발행·수신
│       ├── exception/   ├── mapper/  ├── repository/
│       ├── service/{,processor,validator}/
│       └── sse/{,dto,local,redis/{,pubsub,stream},store}/
└── global/
    ├── ai/          config/{,property}/   common/{,pagenation}/   enums/
    ├── exception/{,model}/   filter/   infra/{redis,s3}/   outbox/
    ├── s3/{enums,exception,service}/      security/{,jwt}/
    └── util/{idempotency,timetrace}/
```

**규칙**

- 도메인 패키지 내부 구성은 위와 같이 `controller / service / repository / entity / dto / mapper / exception / enums`를 기본으로 하되, **필요한 것만 만든다** (예: `auth`는 엔티티가 없어 `entity`·`repository`가 없다).
- 새로운 도메인(예: `university` — 전국 대학 확장에 따른 대학 엔티티/연관관계 관리)은 **`com.skulikelion.festival.domain.university`** 아래에 같은 구성으로 만든다. 새 도메인 추가는 이슈+ADR을 거친다 (스키마 개편 수반 시 반드시 ADR).
- **여러 도메인이 공유하거나 도메인에 속하지 않는 코드는 `global` 아래에 둔다.** 멱등성·아웃박스·페이지네이션·보안·설정이 여기 해당한다.
- `service`가 `repository`를 직접 주입받아 사용하는 현재 방식을 유지한다. 인터페이스/구현체 분리(`XxxService` + `XxxServiceImpl`)는 기존 관례를 따르되, 의존성 역전은 이번 범위에서 강제하지 않는다.
- 도메인 간 참조가 필요하면(예: `order`가 `booth` 정보를 참조) 상대 도메인의 `service`를 호출하는 방식을 기본으로 하되, CQRS·Read Model 도입 이후의 참조 방식은 해당 ADR에서 별도로 정의한다.
- 전략이 여러 개인 기능(멱등성 4전략, SSE 3방식, 이벤트 2방식)은 **인터페이스 + `@ConditionalOnProperty` 빈 등록**으로 교체 가능하게 두는 기존 패턴을 따른다. 구현체를 `@Component`로 직접 달지 말고 `global/config`의 `@Configuration`에서 조립한다.

### 3.2 CQRS / Read Model 도입 시 확장 규칙 (1.4의 4번 작업부터 적용)

Read Replica·CQRS·MongoDB Read Model을 도입하는 시점부터는 아래 규칙을 따른다. **도입 이전 코드에는 소급 적용하지 않으며, 해당 ADR/LLD가 병합된 도메인부터 순차 적용한다.**

- 조회 전용 로직은 `service` 내부에 섞지 않고 `query` 하위 패키지(예: `domain/order/query/OrderQueryService`)로 분리한다.
- MongoDB를 사용하는 Read Model 문서(도큐먼트) 클래스는 별도 `readmodel` 패키지에 두어, MySQL 기반 JPA 엔티티(`entity`)와 명확히 구분한다.
- Read Model 갱신 방식(동기 갱신 / 이벤트 기반 비동기 갱신)은 도입 시 ADR로 확정한다. **이미 있는 아웃박스(`global/outbox`)를 갱신 트리거로 재사용할 수 있는지를 옵션에 반드시 포함한다.**
- Read Replica로의 라우팅(읽기 트랜잭션 분리)은 설정 방식을 ADR로 확정하고, LLD에 어느 API가 Replica를 사용하는지 명시한다.

### 3.3 대량 데이터 작업 관련 규칙

- 더미 데이터 생성 스크립트는 운영 코드(`src/main`)와 분리하여 별도 위치(예: `scripts/` 또는 `src/test` 하위 도구성 디렉터리)에 둔다. 위치는 최초 도입 시 ADR 또는 LLD에서 확정한다.
- 데이터 규모·분포 조건은 항상 사용자가 지정한 값을 그대로 LLD에 기록하고, 에이전트가 임의로 추정하거나 대체하지 않는다.
- 배치 삽입/프로시저 등 삽입 방식 선택, 트랜잭션 크기, 실패 시 재시도 전략은 LLD에 명시한다.
- 인덱스 추가/변경은 반드시 실측(`EXPLAIN` 실행 계획, 응답시간 비교) 근거를 ADR 또는 LLD에 남긴다.
- 스키마 변경은 **항상 새 Flyway 마이그레이션 파일(`V{N+1}__<snake_case_설명>.sql`)로 추가**한다. 이미 적용된 마이그레이션 파일을 수정하지 않는다(체크섬 불일치로 기동 실패).

## 4. 산출물 요약

| 산출물 | 위치 | 근거 규칙 |
|---|---|---|
| 개발 방침 (필독) | `docs/policy/development-policy.md` | 변경은 ADR 경유 |
| 이슈 | GitHub Issues | `.agents/skills/issue/SKILL.md`, `.github/ISSUE_TEMPLATE/*.md` |
| ADR | `docs/adr/ADR-NNNN-*.md` + 사본 `C:\code\study\festival\adr\` | `docs/templates/adr.md`, 워크플로 ⑥ — 옵션 **2~3개** 비교, **작성 후 ②-1 개발자 승인 필수**, `docs/adr/README.md` 인덱스 갱신 필수 |
| LLD | `docs/lld/LLD-NNNN-*.md` + 사본 `C:\code\study\festival\lld\` | `docs/templates/lld.md`, 워크플로 ⑥ — **작성 시 `docs/lld/README.md` 인덱스 갱신 필수** |
| API 스펙 | `docs/api-spec/<domain>.md` | `docs/api-spec/api-conventions.md` — 컨트롤러 개발 전 작성, 구현과 일치 유지 |
| ERD | `docs/erd/erd-NNNN-*.md` | 스키마 개편(전국 대학 확장 등) 시 갱신 필수 |
| DB 마이그레이션 | `src/main/resources/db/migration/V{N}__*.sql` | 추가만 허용, 기존 파일 수정 금지 |
| 더미데이터/부하테스트 스크립트 | `scripts/` 등 (3.3 참조) | 실행 결과(건수, 소요시간, 측정 지표)를 이슈/문서에 기록 |
| 테스트 + 구현 코드 | `src/test`, `src/main` | `.agents/skills/tdd-red/SKILL.md`, `.agents/skills/tdd-green/SKILL.md`, `.agents/skills/tdd-refactor/SKILL.md` |
| 커밋 | git history | `.agents/skills/commit/SKILL.md` |
| PR | GitHub Pull Requests (base: `develop`) | `.agents/skills/pr/SKILL.md`, `.github/pull_request_template.md` |

## 5. 검증 명령

```bash
./gradlew test          # 전체 테스트 (커밋/PR 전 필수 통과)
./gradlew build         # 빌드 검증 (CI가 도는 것과 동일)
./gradlew spotlessApply # 포맷 정리 — CI의 spotlessCheck 실패 시
```

- **Windows(PowerShell)에서는 `.\gradlew.bat test` 형태로 실행한다.**
- **통합 테스트는 Testcontainers를 쓰므로 Docker Desktop이 실행 중이어야 한다.**
  `IntegrationTestSupport`가 MySQL 8.0 + Redis 7-alpine 컨테이너를 띄운다. Docker가 꺼져 있으면
  `DbIdempotentServiceIntegrationTest` / `RedisIdempotentServiceIntegrationTest`가 `initializationError`로 실패한다 — 코드 문제가 아니다.
- `compileJava`가 `spotlessApply`를 선행하므로, 위 명령을 돌리면 작업 트리의 소스 파일이 변경될 수 있다.