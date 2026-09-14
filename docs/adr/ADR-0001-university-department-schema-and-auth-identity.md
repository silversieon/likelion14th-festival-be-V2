# ADR-0001: 대학·학과를 테이블로 승격하고, 인증 식별자를 `Department` enum 이름에서 대리 키로 바꾼다

> ✅ **2026-09-14 개발자 승인 완료 — 옵션 2로 확정.** 승인 내용은 문서 맨 아래 "개발자 응답 기록"을 따른다.

| 항목 | 내용 |
|---|---|
| 상태 | **승인됨** |
| 승인 | ✅ 승인 (2026-09-14, 선택된 옵션: **2**) |
| 작성일 | 2026-09-14 |
| 작성자 | 코딩 에이전트 (Claude) |
| 관련 이슈 | [#3](https://github.com/silversieon/likelion14th-festival-be-V2/issues/3) |
| 관련 도메인 | university(신설) · auth · booth · manager · global |
| 작업 갈래 | ① 전국 대학 확장 (AGENTS.md 1.4) |
| 관련 방침 | `docs/policy/development-policy.md` **4.4**(전국 확장 시 반드시 다룰 것), **4.2·4.3**(스키마 컨벤션·파괴적 변경), **13.1·13.2**(인증·자원 소유권) |
| 스키마 영향 | **있음** (`V14__*.sql` 추가 + `docs/erd/` 신규 문서 필요) |

## 컨텍스트 (Context)

### 지금 무엇이 문제인가

이 서비스는 **단일 대학(서경대) 하나만** 존재한다는 전제로 설계되었다. 그 전제가 코드 한 곳에 응축되어 있다 — `global/enums/Department` **Java enum**이다.

```java
// src/main/java/com/skulikelion/festival/global/enums/Department.java
public enum Department {
  SOFTWARE("소프트웨어학과"),
  BEAUTY("미용예술학부"),
  // ... 서경대 학과·자치기구 30여 개가 코드에 하드코딩되어 있다
}
```

이 enum이 **데이터 식별자이면서 동시에 인증 식별자**로 쓰이고 있다. 세 곳에서 같은 값을 쓴다:

| 위치 | 현재 형태 | 역할 |
|---|---|---|
| `booth.department` | `VARCHAR(100)` **UNIQUE** NOT NULL | 부스의 정체성. "한 학과 = 한 부스" |
| `managers.department` | `VARCHAR(100)` **UNIQUE** NOT NULL | **로그인 ID 겸용** |
| JWT `subject` | `"SOFTWARE"` 같은 enum 이름 문자열 | 인증 주체 |

그리고 "이 매니저가 이 부스의 주인인가"는 **물리 FK 없이 문자열 비교**로 판정한다. `docs/erd/erd-0001-initial-schema.md` 3.3이 지적하듯, 두 테이블 사이에 **유일하게 FK가 없는 연결**이다.

```java
// OrderServiceImpl.validateBoothManagerBelongsToBooth — 같은 패턴이 3개 서비스에 중복되어 있다
Department department = Department.valueOf(departmentName);      // ① 토큰 문자열 → enum 파싱
Manager currentManager = managerRepository.findByDepartment(department)...;  // ② 매니저 조회
if (!booth.getDepartment().equals(currentManager.getDepartment())) { ... }   // ③ enum 값 비교
```

### 전국 확장을 시작하면 정확히 무엇이 깨지는가

`docs/policy/development-policy.md` 4.4와 `docs/erd/erd-0001-initial-schema.md` 6.1이 이미 넷으로 정리해 둔 것이다.

| # | 깨지는 것 | 왜 |
|---|---|---|
| 1 | `booth.department` UNIQUE | A대학 소프트웨어학과와 B대학 소프트웨어학과가 **같은 값**이 된다. 두 번째 부스 INSERT가 제약 위반으로 실패한다 |
| 2 | `Department` enum | 전국 대학의 학과를 enum에 넣을 수 없다. 학과가 늘 때마다 **배포**가 필요해진다 |
| 3 | JWT subject = enum 이름 | enum이 사라지면 `Department.valueOf(token.getSubject())`가 도는 인증 경로 전체가 무너진다 |
| 4 | `managers.department` ↔ `booth.department` | 동명 학과가 생기는 순간 **다른 대학 부스의 소유자로 판정**될 수 있다 → **보안 결함** |

policy 4.4는 이 넷을 **"한 덩어리로 묶어 하나의 ADR로 결정한다. 따로 고치면 중간 상태가 깨진다"** 고 못 박았다. 이 ADR이 그것이다.

### 사용자가 지정한 스키마 (원문 인용 — 에이전트가 임의로 바꾸지 않는다)

> "University 테이블과 Department 테이블이 필요하며 두 테이블은 1:N 관계를 맺게 돼. University 테이블은 id, 학교명, 시도명(경기도, 경상남도 서울특별시 등) 세 컬럼을 가지고, 학과 테이블은 id, university_id, 학과명 컬럼을 가지게 돼. 그리고 각 부스는 Department와 1:1 연관관계를 맺는 형태가 되는 거지."

즉 **테이블 구조 자체는 결정된 입력**이다. 이 ADR이 실제로 결정해야 하는 것은 그 위에 얹히는 다음 항목이다.

> "기존에는 Department만을 아이디로 지정하고 인증, 인가를 진행했는데 이제 한 학교에 서비스가 종속되지 않으니 학교, 학과를 기준으로 부스 관리자로 로그인하는 방식이 필요해. 이 때 학교+학과로 인증, 인가를 할 수 있는 방식 중 어느것이 좋을지 생각해서 제안하는 것이 필요해."

### 측정된 현상

**이 ADR은 성능 결정이 아니라 구조·보안 결정이므로 실측 표가 비어 있다.** (`docs/adr/README.md`의 "성능 ADR은 실측 없이 승인 불가" 규칙은 성능 결정에 적용된다.)

다만 인가 경로의 **비용 변화**는 코드에서 세어 확인할 수 있으므로 아래에 남긴다. 본격적인 조회 성능 측정은 AGENTS.md 1.4-③의 대상이며 이 ADR의 범위가 아니다.

| 항목 | 값 |
|---|---|
| 대상 경로 | 부스 관리자 인가 (`validateBoothManager` / `validateBoothManagerBelongsToBooth` / `LocalOrderSseSubscriber`) |
| 현재 요청당 비용 | `Department.valueOf()` 파싱 1회 + `managers` **UNIQUE 인덱스 조회 1회** + enum 동등 비교 1회 |
| 중복 구현 지점 | **3곳** (`BoothServiceImpl`, `BoothMenuServiceImpl`, `OrderServiceImpl`) + SSE 구독 1곳 = 총 4곳에 같은 로직이 복제되어 있음 |
| 영향 받는 컨트롤러 시그니처 | `@AuthenticationPrincipal String departmentName` — **18개 지점** (booth 5, menu 3, manager 1, order 8, sse 1) |
| 현행 데이터 규모 | 서경대 학과·자치기구 기준 **부스 30여 개 / 매니저 30여 개** (전국 확장 전) |

## 결정 동인 (Decision Drivers)

우선순위 순이다.

1. **소유권 판정의 정확성 (보안)** — 동명 학과가 서로의 부스를 조작할 수 없어야 한다. policy 13.2가 이미 "확인된 누락"을 갖고 있는 영역이라 여기서 부채를 더 쌓으면 안 된다.
2. **식별자의 안정성** — 학과명·대학명은 **바뀔 수 있는 이름(자연 키)** 이다. 인증 토큰이 자연 키에 묶이면 이름이 바뀔 때마다 발급된 토큰이 전부 무효가 되고 소유권 판정이 어긋난다.
3. **확장성** — 학과를 추가하는 데 **배포가 필요 없어야** 한다. 전국 규모에서 학과는 수만 건이 된다.
4. **변경 범위와 프런트 계약 영향** — 컨트롤러 18개 지점과 로그인 요청 계약이 걸려 있다. 넓을수록 이번 PR이 커진다.
5. **기존 코드 관례 유지** — AGENTS.md 3.1의 도메인형 패키지 구조, `service`가 `repository`를 직접 주입하는 방식을 그대로 따른다. 사용자 요청: *"기존 방식과 유사하게, 필요 시에 기존 코드를 변경하지만 요청하지 않은 부분에 대해서는 많이 바꾸지마."*

## 공통 전제 — 세 옵션이 모두 공유하는 스키마

옵션이 갈리는 지점은 **인증 식별자**이지 테이블 구조가 아니다. 아래 스키마는 사용자 지시이며 세 옵션 모두 동일하게 깐다.

```
universities (1) ──< departments (N) ──(1:1)── booth
```

| 테이블 | 컬럼 | 비고 |
|---|---|---|
| `universities` | `id` BIGINT PK, `name` VARCHAR(100) NOT NULL, `region` VARCHAR(50) NOT NULL | `region` = 시도명. 17개로 고정이므로 **`Region` enum + `VARCHAR` 저장**을 권고 (ERD 2장 관례: MySQL `ENUM` 대신 `VARCHAR` + `@Enumerated(STRING)`) |
| `departments` | `id` BIGINT PK, `university_id` BIGINT NOT NULL FK → `universities(id)`, `name` VARCHAR(100) NOT NULL | **UNIQUE (`university_id`, `name`)** — 이것이 "동명 학과 공존"을 보장하는 핵심 제약 |
| `booth` | `department_id` BIGINT **UNIQUE** FK → `departments(id)` 추가 | UNIQUE가 곧 **1:1**. 기존 `booth.department` VARCHAR 컬럼은 대체된다 |

- 신규 테이블 이름은 policy 4.2 / ERD 2장에 따라 **복수형 `snake_case`** (`universities`, `departments`).
- `departments` 테이블 이름과 Java enum `Department`의 이름이 충돌하므로, **엔티티 `Department`를 `domain/university/entity/`에 두고 `global/enums/Department` enum은 제거**한다. 같은 이름 두 개를 공존시키지 않는다.
- 패키지는 AGENTS.md 3.1의 지시대로 **`com.skulikelion.festival.domain.university`** 하나에 `University`와 `Department`를 함께 둔다. (두 엔티티는 1:N으로 강하게 묶여 있고, `Department` 단독 도메인을 만들 만한 독립 유스케이스가 아직 없다.)

> **기존 방식과의 차이 한 줄**: 지금은 "학과 = 코드에 박힌 enum 상수"였고, 앞으로는 "학과 = `departments` 테이블의 한 행"이 된다. 학과를 늘리는 일이 **배포에서 INSERT로** 바뀐다.

### `managers`도 함께 바뀐다

세 옵션 모두 `managers.department` VARCHAR UNIQUE를 버리고 **`managers.department_id` BIGINT FK**를 갖는다. 이로써 ERD 4장에서 점선(`..`)으로 그려져 있던 **유일한 "FK 없는 참조"가 실선이 된다.**

```
managers.department_id ──> departments.id <── booth.department_id
```

소유권 판정은 이제 `booth.department_id == manager.department_id` **정수 비교**다. 현재의 "enum 파싱 + 문자열 비교"가 사라진다.

## 고려한 옵션 (Considered Options)

세 옵션은 모두 위 스키마를 전제하며, **JWT의 subject에 무엇을 담고 컨트롤러가 어떤 principal을 받을 것인가**에서만 갈린다.

---

### 옵션 1: 합성 로그인 ID 문자열 (`managers.login_id`)

- **설명**: `managers`에 `login_id VARCHAR(150) UNIQUE`를 추가하고 `"SKU__SOFTWARE"`처럼 **대학과 학과를 합친 문자열**을 넣는다. 로그인 요청은 지금과 같이 `{loginId, password}` 한 쌍이고, JWT subject에는 `login_id`가 들어간다. 컨트롤러는 `@AuthenticationPrincipal String loginId`를 **지금 그대로** 받는다.
- **장점**:
    - **변경 범위가 가장 작다.** `JwtProvider`, `JwtAuthenticationFilter`, `CustomUserDetails(Service)`가 여전히 `String`을 다루므로 구조가 그대로다. 컨트롤러 18개 지점의 시그니처는 파라미터 이름만 바뀐다.
    - 프런트 로그인 화면이 **입력 필드 하나**로 유지된다. 계약 변경이 사실상 없다.
    - `Department` enum은 제거되므로 확장성 문제(동인 3)는 해결된다.
- **단점**:
    - **자연 키 의존이 그대로 남는다.** enum 상수 이름이 "사람이 정한 합성 문자열 규약"으로 바뀌었을 뿐이다. 대학 통폐합·학과명 변경 시 `login_id`를 고쳐야 하고, 그 순간 발급된 토큰이 무효가 된다 (동인 2 실패).
    - **매 인가 요청마다 `login_id`로 `managers`를 다시 조회**해야 `department_id`를 알 수 있다. 현재 비용이 그대로 유지된다 (개선 없음).
    - 규약을 누가 강제하는가가 불명확하다. 구분자(`__`)가 학과명에 섞이면 파싱이 깨진다. **DB 제약으로 표현할 수 없는 규칙**이 하나 늘어난다.
    - 사용자 요구인 *"학교, 학과를 기준으로 로그인"* 을 형식적으로만 만족한다 — 관리자는 여전히 **조합된 ID 문자열을 외워야** 한다.
- **예상 효과**: 인가 경로 비용 변화 없음(조회 1회 유지). 이번 PR 변경 파일 추정 **약 20개**.

---

### 옵션 2: (대학, 학과) 복합 로그인 + JWT subject = `manager.id` — **권고안**

- **설명**:
    - 로그인 요청이 `{universityId, departmentId, password}`가 된다 (프런트는 대학 → 학과를 순서대로 고른다). 서버는 `managers.department_id`로 계정을 특정한다.
    - JWT subject에는 **`manager.id`(대리 키)** 를 담고, 추가 클레임으로 `departmentId` / `universityId`를 싣는다.
    - `@AuthenticationPrincipal`이 받는 principal을 `String`에서 **`AuthPrincipal` record**(`managerId`, `departmentId`, `universityId`, `role`)로 승격한다.
    - 소유권 판정은 `booth.getDepartment().getId().equals(principal.departmentId())` — **DB 조회 없는 정수 비교**가 된다.
- **장점**:
    - **자연 키가 토큰에서 완전히 사라진다.** 학과명이 바뀌어도 `department_id`는 그대로이므로 발급된 토큰과 소유권 판정이 흔들리지 않는다 (동인 2 충족).
    - **동명 학과 충돌이 원천적으로 불가능**하다. `department_id`는 대학까지 포함해 유일하기 때문이다 (동인 1 충족).
    - 인가 시 **`managers` 조회 1회가 사라진다.** 토큰 클레임에 `departmentId`가 이미 있다. 관리자 화면은 SSE로 오래 떠 있고 주문 목록을 반복 조회하므로, 요청당 조회 1회 제거는 피크에 의미가 있다.
    - 4곳에 복제된 소유권 검증 로직을 **한 곳으로 모을 자연스러운 계기**가 된다 (사용자의 "많이 바꾸지 마" 요청 범위 안 — 인증/인가는 이번 작업의 명시적 대상이다).
    - 인증 주체(**계정**)와 자원(**학과/부스**)이 토큰에서 분리된다. 나중에 한 학과에 매니저 계정이 여러 개 필요해져도 토큰 구조를 다시 바꾸지 않는다.
- **단점**:
    - **변경 범위가 가장 넓다.** `@AuthenticationPrincipal String departmentName` **18개 지점**과 그 아래 서비스 메서드 시그니처가 전부 바뀐다.
    - **프런트 로그인 계약이 바뀐다.** 요청 본문이 `{departmentName, password}` → `{universityId, departmentId, password}`가 되고, **대학·학과 목록 조회 API가 선행되어야** 프런트가 id를 보낼 수 있다. (그 조회 API는 이번 이슈 범위 밖이므로 후속 작업이 된다 — 열린 질문 ②)
    - **기존에 발급된 토큰이 전부 무효**가 된다. 전원 재로그인이 필요하다.
    - `UserDetailsService.loadUserByUsername(String)`이 단일 문자열만 받으므로, 복합 자격을 다루려면 `AuthenticationProvider`를 직접 쓰거나 `username` 자리에 `manager.id` 문자열을 넣는 어댑팅이 필요하다. **Spring Security 계약과 살짝 어긋나는 지점**이다.
- **예상 효과**: 인가 경로 요청당 **`managers` 조회 1회 제거**. 이번 PR 변경 파일 추정 **약 40개**.

---

### 옵션 3: JWT subject = `department_id` (부스 자원 자체를 주체로)

- **설명**: 옵션 2와 같되, 토큰 subject를 `manager.id`가 아니라 **`department_id`** 로 둔다. principal은 단일 `Long departmentId`다. `managers.department_id`가 UNIQUE이므로 지금은 계정과 학과가 1:1이라 정보 손실이 없다.
- **장점**:
    - 옵션 2의 장점(자연 키 제거·동명 학과 충돌 차단·조회 1회 제거)을 **거의 그대로** 얻는다.
    - principal이 **단일 `Long`** 이라 컨트롤러 변경이 `String` → `Long`으로 끝난다. record를 새로 만들 필요가 없어 **옵션 2보다 변경 폭이 작다.**
    - 현재 코드가 `departmentName` 하나를 들고 다니는 모양과 **가장 비슷하다.** 기존 관례 유지(동인 5)에 유리하다.
- **단점**:
    - **인증 주체(계정)와 자원(학과)이 토큰에서 동일시된다.** "누가 로그인했는가"를 토큰만 보고 알 수 없다.
    - 한 학과에 **매니저 계정이 둘 이상** 필요해지는 순간(부스 운영 인력 분담, 계정별 감사 로그) 토큰 구조를 다시 바꿔야 한다. 현재 `managers.department` UNIQUE라는 가정은 **단일 대학이라 성립했던 것**이고, 전국 확장 후에도 유지된다는 보장이 없다.
    - 로그인 시 `role`을 클레임으로 싣더라도, 계정 단위로 하고 싶은 일(비밀번호 변경 이력, 마지막 로그인 시각)은 별도 조회가 필요하다.
- **예상 효과**: 인가 경로 요청당 `managers` 조회 1회 제거(옵션 2와 동일). 이번 PR 변경 파일 추정 **약 35개**.

---

### 비교 요약

| 기준 | 옵션 1 (합성 login_id) | 옵션 2 (manager.id) | 옵션 3 (department_id) |
|---|---|---|---|
| 소유권 판정 정확성 (동인 1) | 조회 후 id 비교 — 정확 | **정확 · 조회 불필요** | **정확 · 조회 불필요** |
| 식별자 안정성 (동인 2) | ❌ 자연 키 의존 잔존 | ✅ 대리 키 | ✅ 대리 키 |
| 확장성 — 학과 추가에 배포 불필요 (동인 3) | ✅ | ✅ | ✅ |
| 인가 요청당 DB 조회 | 1회 (현행 유지) | **0회** | **0회** |
| 컨트롤러 변경 지점 | 18곳 (이름만) | 18곳 (타입 변경) | 18곳 (`String`→`Long`) |
| 프런트 로그인 계약 변경 | 거의 없음 | **있음 (필드 2개 + 선행 조회 API)** | **있음 (필드 2개 + 선행 조회 API)** |
| 기존 토큰 무효화 | 발생 | 발생 | 발생 |
| 계정 다중화 대비 | 가능 | ✅ 가능 | ❌ 재설계 필요 |
| 운영 복잡도 | 낮음 (단, 문자열 규약 관리 필요) | 중간 | 낮음 |
| 되돌리기 난이도 | 낮음 | 중간 | 중간 |

## 결정 (Decision)

**권고안**: **옵션 2** — 결정 동인 1·2·3을 모두 만족하는 유일한 안이고, 이번 개편의 목적이 "단일 대학 전제를 제거하는 것"인 이상 **가장 오래 버티는 식별자(대리 키 + 계정/자원 분리)** 를 지금 고르는 편이 총비용이 낮기 때문이다.

근거를 셋으로 풀면:

1. **옵션 1은 문제를 해결하지 않고 옮긴다.** enum 하드코딩을 문자열 규약으로 바꾼 것이고, policy 4.4가 지적한 "JWT subject가 자연 키" 문제(3번 항목)는 그대로 남는다. 전국 확장이 끝난 뒤 다시 손대야 할 가능성이 높다.
2. **옵션 3과 옵션 2의 차이는 "지금의 편의"와 "나중의 자유도"다.** 옵션 3이 변경 폭이 조금 작지만, `managers.department` UNIQUE(= 학과당 계정 1개)라는 가정은 **단일 대학이었기 때문에 성립했던 것**이다. 전국 규모에서 이 가정이 유지된다는 근거가 없고, 깨지는 순간 토큰 구조를 또 바꾸게 된다. 파일 5개 차이로 그 리스크를 사는 것은 이득이 아니다.
3. 옵션 2는 **인가 요청당 `managers` 조회 1회를 제거**한다. 부스 관리자 화면은 주문 목록을 반복 조회하고 SSE를 길게 물고 있으므로(policy 2.1), 피크에 이 차이가 누적된다.

**확정**: **옵션 2** (개발자 승인 2026-09-14) — 권고안과 동일하다.

### 구현 규칙 (LLD와 리뷰가 이 목록을 근거로 삼는다)

- 패키지는 **`com.skulikelion.festival.domain.university`** 하나를 신설하고 `entity/`, `repository/`, `exception/`을 둔다. AGENTS.md 3.1대로 **필요한 것만** 만든다 — 이번 범위에 조회 API가 없으므로 `controller`·`dto`·`service`는 만들지 않는다.
- `global/enums/Department` enum은 **삭제**한다. 같은 이름의 엔티티와 공존시키지 않는다.
- `Booth`는 `@OneToOne`으로 `Department`를 참조하고 컬럼은 `department_id`(UNIQUE FK)다. `Booth`에 `university_id`를 **중복해서 두지 않는다** — `booth → department → university`로 도달한다. (조회 성능 때문에 비정규화가 필요해지면 AGENTS.md 1.4-③에서 실측 후 별도 ADR로 결정한다. policy 5.1 "실측 없는 최적화 금지".)
- `Manager`는 `@ManyToOne`으로 `Department`를 참조하고 컬럼은 `department_id`다. **DB에는 UNIQUE를 유지**한다 (열린 질문 ⑤ 확정: 학과당 매니저 계정 1개라는 현행 동작을 그대로 보존).
  JPA를 `@ManyToOne`으로 두는 이유는, 나중에 이 UNIQUE를 푸는 결정이 나더라도 **토큰 구조와 엔티티 매핑을 다시 바꾸지 않기 위해서**다. 옵션 2를 고른 실익이 여기서 나온다.
- `universities.region`은 `Region` enum(17개 광역시·도) + `@Enumerated(EnumType.STRING)` + `VARCHAR(50)`으로 매핑한다 (열린 질문 ④ 확정. ERD 2장 관례 — MySQL `ENUM` 대신 `VARCHAR` + `@Enumerated(STRING)`).
- `universities`의 UNIQUE는 **`(name, region)` 복합**으로 건다 (열린 질문 ③ 확정).
- Flyway는 **`V14__create_universities_and_departments.sql` 하나**로 끝낸다. 이미 적용된 `V1`~`V13`은 건드리지 않는다 (policy 4.1).
- **기존 데이터는 백필로 보존한다** (열린 질문 ① — 2026-09-14 개발자 번복 확정). policy 4.3의 다섯 단계(① 컬럼 추가(nullable) → ② 백필 → ③ 전환 → ④ NOT NULL/제약 → ⑤ 옛 컬럼 제거)를 **한 파일 안에서 순서대로** 수행한다.
    - 기존 `booth.department` / `managers.department` 값은 전부 **서경대 한 학교의 `Department` enum 이름**이므로 대학이 하나로 확정된다. `universities`에 서경대학교 1행, `departments`에 enum 33개를 행으로 넣고 조인해 백필한다.
    - **한글 학과명(`description`)은 DB에 없고 Java enum에만 있다.** 따라서 enum 33쌍(`이름` ↔ `한글 학과명`)을 **마이그레이션 SQL에 값으로 써 넣는다.** 코드에 있던 데이터를 DB로 옮기는 것이 이번 개편의 본질이므로 이것이 정당한 위치다.
    - 매핑은 `departments.legacy_code`(임시 컬럼, 마이그레이션 끝에서 DROP)로 잇는다. **매핑되지 않는 행이 있으면 `NOT NULL` 전환에서 마이그레이션이 실패**하게 두어, 조용한 데이터 유실 대신 실패를 택한다.
    - **파일을 나누지 않는 이유**: policy 4.3이 다단계를 요구하는 목적은 구/신 애플리케이션 버전이 공존하는 무중단 배포를 가능케 하는 것이다. 이번 변경은 **토큰 형식이 바뀌어 구·신 버전이 애초에 공존할 수 없고**, 대상 테이블도 30여 행이라 잠금 시간이 문제되지 않는다. 파일을 쪼개도 Flyway가 기동 시 한꺼번에 적용하므로 얻는 것이 없다.
    - **되돌릴 수 없다.** Flyway에 `undo`가 없으므로 롤백은 백업 복원이다. 실행 전 `booth` / `managers` / 주문 계열 백업을 권고한다.
- **로그인 요청은 `{universityId, departmentId, password}`** — id로 받는다 (열린 질문 ② 확정). 프런트가 id를 얻기 위한 **대학·학과 목록 조회 API는 이번 범위 밖**이며 후속 이슈로 분리한다.
- 소유권 검증 로직은 **한 곳(`global/security` 또는 공용 검증 컴포넌트)으로 모은다.** 현재 4곳에 복제되어 있고, principal 타입이 바뀌면 4곳을 똑같이 고쳐야 하기 때문이다.
- 구현은 TDD Red → Green → Refactor 사이클로 진행하며, 하위 계층(repository) → service → controller 순으로 간다 (AGENTS.md ④).
- `docs/api-spec/auth.md`를 **컨트롤러 구현 전에** 작성한다 (AGENTS.md ③ "API 스펙 선행 작성"). 로그인 요청 본문이 바뀌기 때문이다.

## 결과 (Consequences)

### 개선 실측치

성능 결정이 아니므로 Before/After 부하 측정은 수행하지 않는다. 구조적 변화만 기록한다.

| 항목 | Before | After (옵션 2 기준) |
|---|---|---|
| 학과 식별 방식 | Java enum 상수 30여 개 (하드코딩) | `departments` 테이블 행 (무제한) |
| 학과 추가 비용 | **코드 수정 + 배포** | `INSERT` 1건 |
| 동명 학과 공존 | **불가** (`booth.department` UNIQUE 위반) | 가능 (`UNIQUE(university_id, name)`) |
| JWT subject | `"SOFTWARE"` (자연 키) | `manager.id` (대리 키) |
| 인가 요청당 DB 조회 | `managers` 조회 1회 | **0회** (토큰 클레임 사용) |
| `managers` ↔ `booth` 연결 | 문자열 값 일치 (FK 없음) | **둘 다 `departments.id` FK** |
| 소유권 검증 로직 위치 | 4곳에 복제 | 1곳 |

### 긍정적

- policy 부록 **"⚠️ 해결 대상 #14"**(`Department` enum 하드코딩 / `booth.department` UNIQUE)가 해소된다.
- ERD 4장에서 **점선으로 그려져 있던 유일한 "FK 없는 참조"가 실선**이 된다. DB가 소유권 관계를 강제하게 된다.
- 학과·대학이 데이터가 되므로, AGENTS.md 1.4-②의 **대량 데이터 적재**가 가능해진다. 이 ADR은 그 작업의 선행 조건이다.

### 부정적 / 트레이드오프

- **프런트 계약이 깨진다.** 로그인 요청 본문이 바뀌고, 대학·학과를 고르려면 **목록 조회 API가 먼저 필요**하다. 그 API는 이번 이슈 범위 밖이라 **프런트가 바로 붙을 수 없는 기간이 생긴다.**
- **발급된 토큰이 전부 무효**가 된다. 배포 시점에 전원 재로그인이 필요하다.
- **이번 PR이 크다.** 컨트롤러 18개 지점 + 서비스 4곳 + 인증 5개 클래스 + 엔티티 3개 + 마이그레이션 + 문서. 사용자 요청("많이 바꾸지 마")과 긴장이 있으나, principal 타입 변경은 컴파일이 강제하는 연쇄라 **부분 적용이 불가능**하다.
- `booth_translation.department_name`(다국어 학과명)이 **`departments.name`과 의미가 겹친다.** 이번 범위에서는 건드리지 않고 그대로 두지만, 학과명의 단일 출처가 두 곳이 되는 상태가 남는다 → 후속 작업.
- `UserDetailsService.loadUserByUsername(String)`에 복합 자격을 태우는 어댑팅이 들어간다. Spring Security의 원래 계약과 살짝 어긋나며, 코드에 그 이유를 주석으로 남겨야 한다.

### 후속 작업

- [ ] **ERD 신규 문서** `docs/erd/erd-0002-*.md` 작성 + `erd-0001`의 상태를 **"대체됨"** 으로 변경 (AGENTS.md ③)
- [ ] `docs/policy/development-policy.md` **4.4** 표와 **부록 #14**의 "⚠️ 해결 대상" 표시 해제 + 이 ADR 번호 기록 (policy 18장)
- [ ] `docs/api-spec/auth.md` 신규 작성 (로그인·회원가입 요청 계약)
- [ ] **대학·학과 목록 조회 API** — 프런트가 로그인 화면에서 선택하려면 필요하다. 별도 이슈 (이번 범위 밖)
- [ ] **대학·학과 데이터 적재** — AGENTS.md 1.4-②. 분포·규모는 사용자가 지정한다 (policy 3.1)
- [ ] `booth_translation.department_name`과 `departments.name`의 역할 정리
- [ ] `LocalOrderSseEmitterStore.findByBoothId` 전체 스캔 (policy 부록 #13) — 부스가 수천 개가 되는 이번 개편 이후 실제로 문제가 된다. 별도 이슈

## 준수 확인 (Compliance)

- `global/enums/Department`가 **삭제되었는지** — 파일 부재로 확인 (남아 있으면 이 ADR 미준수)
- `grep -r "Department.valueOf" src/main` 결과가 **0건**인지
- **통합 테스트**: 서로 다른 두 대학에 **같은 이름의 학과**를 INSERT하고, 각각 부스를 생성한 뒤, A대학 매니저 토큰으로 B대학 부스의 주문 상태 변경을 시도해 **403이 나오는지** 검증한다. 이것이 이 ADR의 핵심 주장(동인 1)을 증명하는 테스트다.
- 통합 테스트 DB는 실제 제약 동작이 같은 **MySQL(Testcontainers)** 를 쓴다 (policy 8.5).
- 마이그레이션 파일과 새 ERD 문서가 **같은 PR**에 있는지 (policy 4.1)

## 관련 문서

- 관련 ADR: 없음 (첫 ADR)
- 관련 LLD: _승인 후 작성_
- ERD: 현행 `docs/erd/erd-0001-initial-schema.md` (6.1이 이 결정을 예고함) → 신규 문서로 대체 예정
- 마이그레이션: `src/main/resources/db/migration/V14__create_universities_and_departments.sql` (예정)
- 방침: `docs/policy/development-policy.md` 4.2·4.3·4.4·13.1·13.2

---

## ⛔ 개발자 확인 요청 (Approval Gate) — 필수 중단점

### 에이전트 → 개발자 확인 요청

**1. 작성 완료 보고**

- ADR-0001 「대학·학과를 테이블로 승격하고, 인증 식별자를 `Department` enum 이름에서 대리 키로 바꾼다」
- 경로: `docs/adr/ADR-0001-university-department-schema-and-auth-identity.md`
- 관련 이슈: #3 / 브랜치: `feature/3-university-department-schema`

**2. 옵션 요약**

| 옵션 | 얻는 것 | 포기하는 것 |
|---|---|---|
| **1. 합성 로그인 ID** (`"SKU__SOFTWARE"` 문자열, JWT subject = 그 문자열) | 변경 범위 최소 · 프런트 로그인 계약 거의 무변경 · 컨트롤러 시그니처 유지 | 자연 키 의존이 그대로 남음(enum → 문자열 규약으로 이름만 바뀜) · 인가마다 `managers` 조회 1회 유지 · 관리자가 조합 ID를 외워야 함 |
| **2. `manager.id` 대리 키** (로그인 = 대학+학과 선택, principal = `AuthPrincipal` record) | 자연 키 완전 제거 · 동명 학과 충돌 원천 차단 · 인가 시 DB 조회 0회 · 계정 다중화 대비 | 변경 범위 최대(컨트롤러 18곳 + 서비스) · 프런트 로그인 계약 변경 + 대학/학과 조회 API 선행 필요 · 기존 토큰 전부 무효 |
| **3. `department_id` 대리 키** (principal = 단일 `Long`) | 옵션 2의 장점 대부분 · principal이 `Long` 하나라 옵션 2보다 변경 폭 작음 · 현재 코드 모양과 가장 유사 | 계정과 자원이 토큰에서 동일시됨 · **한 학과에 매니저 계정이 둘 이상 필요해지면 토큰 구조 재설계** · 프런트 계약 변경은 옵션 2와 동일하게 발생 |

**3. 권고안과 근거**

**옵션 2**를 권고합니다. 이번 개편의 목적이 "단일 대학 전제의 제거"인데, 옵션 1은 그 전제를 문자열 규약으로 옮길 뿐이고, 옵션 3은 `managers.department` UNIQUE(= 학과당 계정 1개)라는 **단일 대학 시절의 가정**을 토큰 구조에 그대로 박아 넣습니다. 옵션 2와 3의 실질 차이는 파일 5개 정도이며, 그 차이로 "나중에 토큰 구조를 또 바꿀 위험"을 사는 것은 이득이 아니라고 판단했습니다.

다만 **이 판단을 그대로 따르실 필요는 없습니다.** 프런트 일정이 빠듯해 로그인 계약을 지금 못 바꾼다면 **옵션 1**이, 학과당 계정 1개를 확정 정책으로 두실 거라면 **옵션 3**이 충분히 합리적입니다.

**4. 판단이 필요한 열린 질문**

| # | 질문 | 왜 에이전트가 정할 수 없는가 |
|---|---|---|
| ① | **기존 `booth` / `managers` 데이터를 보존해야 하나요?** 보존이라면 `Department` enum 30여 개를 `departments` 행으로 옮기는 **백필**이 필요하고 policy 4.3의 다단계 마이그레이션(추가 → 백필 → 전환 → 제거)으로 나눠야 합니다. 버려도 된다면 **V14 하나로 끝납니다.** | 운영 데이터 존재 여부는 개발자만 압니다. 마이그레이션 파일 개수가 여기서 갈립니다 |
| ② | **로그인 요청에 대학·학과를 `id`로 받을까요, `이름 문자열`로 받을까요?** `id`가 정석이지만 프런트가 id를 알려면 **대학/학과 목록 조회 API가 먼저** 있어야 합니다. 그 API는 이번 이슈 범위 밖입니다 — 이번에 함께 만들지, 다음 계획으로 미룰지 정해 주세요 | 프런트 계약·일정 문제입니다 |
| ③ | **`universities`의 UNIQUE를 `(name)`으로 걸까요, `(name, region)`으로 걸까요?** 캠퍼스가 분리된 대학(예: 서울/제2캠퍼스)을 별도 행으로 둘 거라면 `(name, region)`이어야 합니다. 사용자 지시가 3컬럼이라 코드 컬럼은 두지 않았습니다 | 대학 데이터를 어떻게 끊을지는 데이터 정책입니다 |
| ④ | **시도명(`region`)을 `Region` enum으로 고정할까요, 자유 문자열로 둘까요?** 17개 광역시·도로 고정이면 enum이 오타를 막아 줍니다. 해외 대학·특수 케이스를 열어 둘 거라면 `VARCHAR`가 낫습니다 | 서비스 범위(국내 한정 여부)에 대한 판단입니다 |
| ⑤ | **한 학과에 매니저 계정은 계속 1개인가요?** (`managers.department_id`에 UNIQUE를 걸지 여부) 옵션 3을 고르신다면 이 답이 **반드시 "1개"** 여야 합니다 | 운영 정책입니다. 옵션 3의 성립 조건이기도 합니다 |
| ⑥ | **기존 토큰 무효화(전원 재로그인)를 지금 감당할 수 있나요?** 축제 기간 중이면 곤란합니다 (policy 4.3: 큰 변경의 실행 시점은 축제 기간 외) | 운영 일정 문제입니다 |

### 승인 전에 하지 않는 것

- ❌ LLD 작성
- ❌ 구현 코드·마이그레이션 파일 작성
- ❌ 스터디 저장소 동기화, 커밋, PR 생성
- ❌ 상태를 "승인됨"으로 바꾸기

### 개발자 응답 기록

| 항목 | 내용 |
|---|---|
| 응답 일자 | 2026-09-14 |
| 선택된 옵션 | **옵션 2 — manager.id (권고)** |
| 추가 지시 사항 | 최초 요청 원문 그대로: *"이번 작업에서 해야할 것은 그래서 크게 세 가지야. 1. University, Department 패키지 작성 및 Entity 설계, 2. 변경된 스키마 구조를 기준으로 인증, 인가 방식 설계 및 변경, 3. flyway를 통해서 테이블 추가 sql 작성 이렇게야. 실제 데이터 삽입이나 기능 추가는 다음 계획에서 요청할테니 우선 이번 요구사항에 맞게 코드 작성 해주고, 기존 방식과 유사하게, 필요 시에 기존 코드를 변경하지만 요청하지 않은 부분에 대해서는 많이 바꾸지마."* |
| 열린 질문에 대한 답 | **①** 최초 "보존 불필요 — V14 하나로" → **같은 날 개발자가 번복**: *"혹시 기존 데이터 유지하는 형태로 가능한가"* → **"보존한다 — 백필로 전환"** 확정 / **②** "id로 받기 + 조회 API는 다음 계획" / **③④⑤⑥** 개발자 미응답 → 에이전트가 기본값을 정하고 개발자에게 명시적으로 고지함 (아래) |

**①의 번복 경위 (AGENTS.md ②-1 — 전제가 뒤집혀 ADR을 고치고 재확인했다)**

| 시점 | 내용 |
|---|---|
| 최초 답변 | "보존 불필요 — V14 하나로" |
| 개발자 재질의 | *"혹시 기존 데이터 유지하는 형태로 가능한가"* (원문) |
| 에이전트 회신 | 가능하다고 답하고 백필 방식(서경대 1행 + enum 33행 INSERT → `legacy_code` 조인 백필)과 제약 2가지(① 한글 학과명이 SQL에 값으로 들어감 ② 토큰은 여전히 전부 무효)를 제시 |
| 최종 확정 | **"보존한다 — 백필로 전환"** |

이 번복으로 "결과" 절의 부정적 트레이드오프 중 **"기존 데이터가 사라진다" 항목이 해소**되었다. 토큰 무효화는 그대로 남는다.

**③~⑥ 에이전트 기본값 (개발자 고지 완료, 이의 없을 시 유효)**

| # | 질문 | 정한 값 | 근거 |
|---|---|---|---|
| ③ | `universities` UNIQUE 범위 | **`(name, region)` 복합** | 동명 대학이 다른 시도에 존재할 여지를 남기면서 중복 적재·오타는 막는다 |
| ④ | `region` 타입 | **`Region` enum 17개 고정** | "전국 대학" = 국내 한정. enum이 오타를 컴파일 시점에 막는다. ERD 2장 관례대로 저장은 `VARCHAR(50)` |
| ⑤ | `managers.department_id` UNIQUE | **유지** | 학과당 계정 1개라는 **현행 동작을 그대로 보존**한다. 개발자 지시 *"요청하지 않은 부분에 대해서는 많이 바꾸지마"* 를 따른 것이며, 옵션 2를 골랐으므로 나중에 이 UNIQUE만 풀면 토큰 구조는 건드리지 않아도 된다 |
| ⑥ | 기존 토큰 무효화 시점 | **지금 감당** | ①에서 "기존 데이터 보존 불필요"로 답했으므로 운영 계정이 없는 단계로 판단 |
