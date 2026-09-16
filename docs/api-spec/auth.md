# API 스펙 — auth (인증)

> 공통 규약(응답 envelope `BaseResponse`, 에러 정책, 헤더, 상태 코드)은 [`api-conventions.md`](api-conventions.md)가 단일 기준이다.
> 이 문서는 그 위에서 **auth 도메인의 계약만** 기술한다. envelope 4개 필드는 반복하지 않는다.

| 항목 | 내용 |
|---|---|
| 상태 | 활성 |
| 작성일 | 2026-09-14 |
| 관련 이슈 | [#3](https://github.com/silversieon/likelion14th-festival-be-V2/issues/3) |
| 관련 ADR | [ADR-0001](../adr/ADR-0001-university-department-schema-and-auth-identity.md) — 옵션 2 확정 |
| 관련 LLD | [LLD-0001](../lld/LLD-0001-university-department-and-auth-identity.md) |
| 컨트롤러 | `domain/auth/controller/AuthController` |

## 0. 이 문서가 기록하는 계약 변경 (⚠️ 프런트 필독)

ADR-0001로 **인증 식별자가 바뀌었다.** 기존 계약과 달라지는 지점은 다음 두 가지뿐이며, 그 외(경로·응답 형태·쿠키·메시지)는 전부 그대로다.

| | Before | After |
|---|---|---|
| 로그인·회원가입 요청 본문 | `{ "departmentName": "SOFTWARE", ... }` | `{ "universityId": 1, "departmentId": 12, ... }` |
| JWT `subject` | `Department` enum 이름 (`"SOFTWARE"`) | `manager.id` (`"1024"`) — **클라이언트는 토큰 내부를 파싱하지 않으므로 영향 없음** |

- **`Department` enum은 삭제되었다.** `"SOFTWARE"` 같은 학과 문자열은 더 이상 어떤 API의 입력도 아니다.
- 프런트는 로그인 화면에서 **대학 → 학과 순으로 선택**하고 그 `id` 두 개를 보낸다.
- ⚠️ **대학·학과 목록 조회 API는 이번 범위에 포함되지 않는다.** (ADR-0001 열린 질문 ② 확정 — 후속 이슈) 그때까지 프런트는 id를 하드코딩하거나 DB에서 확인해야 한다.
- ⚠️ **기존에 발급된 토큰은 모두 무효다.** 배포 후 전원 재로그인이 필요하다.

## 1. 회원가입

- **Method / Path**: `POST /api/auth/register`
- **인증**: 불필요 (permitAll) — 대신 `adminKey`로 보호한다
- **멱등성 키**: 불필요
- **Swagger**: `@Tag("Auth")` / `@Operation(summary = "[ 사용자 | 토큰 X | 회원가입 ]")`
- **관련 문서**: LLD-0001, 이슈 #3

### 요청

| 필드 | 타입 | 필수 | 제약 | 설명 |
|---|---|---|---|---|
| universityId | number | Y | `@NotNull` | 대학 식별자 |
| departmentId | number | Y | `@NotNull` | 학과 식별자. **`universityId`에 속한 학과여야 한다** |
| password | string | Y | `@NotBlank` | 비밀번호 (BCrypt로 해시되어 저장) |
| role | string | Y | `@NotNull`, `USER` \| `ADMIN` \| `BOOTH_MANAGER` \| `STUDENT_COUNCIL` | 부여할 역할 |
| adminKey | string | Y | `@NotBlank` | 관리자 키 (`auth.admin-key` 설정값과 일치해야 함) |

```json
{
  "universityId": 1,
  "departmentId": 12,
  "password": "lion1234!",
  "role": "BOOTH_MANAGER",
  "adminKey": "***"
}
```

### 응답 — 201 Created

`data`는 `null`이다.

### 에러

| HTTP | code (내부) | message (실제 응답 문자열) | 발생 조건 |
|---|---|---|---|
| 400 | AUTH4004 | 잘못된 어드민 키 입력 | `adminKey` 불일치 |
| 400 | UNIVERSITY_40002 | 해당 대학에 속한 학과가 아닙니다. | `departmentId`의 소속 대학이 `universityId`와 다름 |
| 404 | UNIVERSITY_40401 | 해당 대학을 찾을 수 없습니다. | `universityId` 없음 |
| 404 | UNIVERSITY_40402 | 해당 학과를 찾을 수 없습니다. | `departmentId` 없음 |
| 409 | AUTH4091 | 이미 해당 학과의 관리자 계정이 존재합니다. | 그 학과에 이미 매니저가 있음 |

## 2. 로그인

- **Method / Path**: `POST /api/auth/login`
- **인증**: 불필요 (permitAll)
- **멱등성 키**: 불필요
- **Swagger**: `@Tag("Auth")` / `@Operation(summary = "[ 사용자 | 토큰 X | 로그인 ]")`
- **관련 문서**: LLD-0001, 이슈 #3

### 요청

| 필드 | 타입 | 필수 | 제약 | 설명 |
|---|---|---|---|---|
| universityId | number | Y | `@NotNull` | 대학 식별자 |
| departmentId | number | Y | `@NotNull` | 학과 식별자. **`universityId`에 속한 학과여야 한다** |
| password | string | Y | `@NotBlank` | 비밀번호 |

```json
{
  "universityId": 1,
  "departmentId": 12,
  "password": "lion1234!"
}
```

### 응답 — 200 OK

`data`는 `null`이다. **토큰은 본문이 아니라 `Set-Cookie` 헤더로 내려간다** (api-conventions 6장).

| 헤더 | 값 |
|---|---|
| `Set-Cookie` | `ACCESS_TOKEN=<JWT>; ...` |
| `Set-Cookie` | `REFRESH_TOKEN=<JWT>; ...` |

**발급되는 Access Token의 클레임** (서버 내부 계약 — 클라이언트가 파싱하지 않는다)

| 클레임 | 값 | 용도 |
|---|---|---|
| `sub` | `manager.id` 문자열 | 인증 주체 |
| `type` | `ACCESS_TOKEN` | 토큰 종류 판별 |
| `role` | `["ROLE_BOOTH_MANAGER"]` | 인가 |
| `departmentId` | number | **자원 소유권 판정** — 이 값 덕분에 매 요청마다 `managers`를 조회하지 않는다 |
| `universityId` | number | 소속 대학 (로깅·향후 대학 단위 인가) |

### 에러

| HTTP | code (내부) | message (실제 응답 문자열) | 발생 조건 |
|---|---|---|---|
| 400 | AUTH4003 | 로그인에 실패했습니다. | 비밀번호 불일치, 또는 해당 학과에 계정이 없음 |
| 400 | UNIVERSITY_40002 | 해당 대학에 속한 학과가 아닙니다. | `departmentId`의 소속 대학이 `universityId`와 다름 |
| 404 | UNIVERSITY_40401 | 해당 대학을 찾을 수 없습니다. | `universityId` 없음 |
| 404 | UNIVERSITY_40402 | 해당 학과를 찾을 수 없습니다. | `departmentId` 없음 |

> **계정 존재 여부가 유추되지 않게** 비밀번호 불일치와 계정 없음을 **같은 메시지(`로그인에 실패했습니다.`)** 로 통일한다 (policy 13.2).

## 3. 토큰 재발급

- **Method / Path**: `POST /api/auth/refresh`
- **인증**: `REFRESH_TOKEN` 쿠키 필요 (**쿠키로만 받는다** — 헤더 불가)
- **Swagger**: `@Tag("Auth")` / `@Operation(summary = "[ 사용자 | 토큰 O | 토큰 재발급 ]")`

### 요청

본문 없음. `Cookie: REFRESH_TOKEN=<JWT>`.

### 응답 — 200 OK

`data`는 `null`. 새 `ACCESS_TOKEN` / `REFRESH_TOKEN` 쿠키를 내려준다 (**Refresh Token Rotation** — 쓴 리프레시 토큰은 Redis에서 삭제하고 새로 발급한다).

### 에러

| HTTP | code (내부) | message (실제 응답 문자열) | 발생 조건 |
|---|---|---|---|
| 401 | AUTH4012 | 유효하지 않은 토큰 입력입니다. | 쿠키 없음 / 서명 위조 / 타입 불일치 / 저장소에 없음 |
| 404 | MANAGER_4041 | 해당 관리자를 찾을 수 없습니다. | 토큰의 `sub`(managerId)에 해당하는 계정이 삭제됨 |

> **계약 변경 없음.** 내부적으로는 `sub`가 학과 이름에서 `manager.id`로 바뀌었지만, 요청·응답 모양은 그대로다.

## 4. 로그아웃

- **Method / Path**: `POST /api/auth/logout`
- **인증**: `REFRESH_TOKEN` 쿠키 필요
- **Swagger**: `@Tag("Auth")` / `@Operation(summary = "[ 사용자 | 토큰 O | 로그아웃 ]")`

### 요청

본문 없음. `Cookie: REFRESH_TOKEN=<JWT>`.

### 응답 — 200 OK

`data`는 `null`. `ACCESS_TOKEN` / `REFRESH_TOKEN` 쿠키를 **0초 만료**로 덮어써 제거하고, Redis에 저장된 리프레시 토큰을 삭제한다.

### 에러

| HTTP | code (내부) | message (실제 응답 문자열) | 발생 조건 |
|---|---|---|---|
| 401 | AUTH4012 | 유효하지 않은 토큰 입력입니다. | 쿠키 없음 / 유효하지 않은 리프레시 토큰 |

> **계약 변경 없음.**

## 5. 인증된 API가 받는 principal (전 도메인 공통)

인증이 필요한 모든 컨트롤러는 `@AuthenticationPrincipal`로 **`AuthPrincipal` record**를 받는다.

```java
public record AuthPrincipal(Long managerId, Long departmentId, Long universityId, Role role) {
  public boolean isAdmin() { return role == Role.ADMIN; }
}
```

| 필드 | 출처 | 쓰임 |
|---|---|---|
| `managerId` | JWT `sub` | 관리자 본인 정보 조회, 로깅 |
| `departmentId` | JWT `departmentId` 클레임 | **자원 소유권 판정** (`booth.department_id`와 비교) |
| `universityId` | JWT `universityId` 클레임 | 소속 대학 |
| `role` | JWT `role` 클레임 | ADMIN 우회 판정 |

- api-conventions 6.2의 *"JWT의 subject는 `Department` enum 이름이며 `@AuthenticationPrincipal String departmentName`으로 받는다"* 는 **이 문서로 대체된다.**
- **"어느 부스의 데이터인가"를 토큰이 결정하는 구조는 그대로 유지된다.** 다만 그 근거가 `Department` enum 문자열에서 `departmentId`(BIGINT)로 바뀌었다.
