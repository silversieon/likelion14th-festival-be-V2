# API 스펙 — university (대학·학과)

> 공통 규약(응답 envelope `BaseResponse`, 에러 정책, 헤더, 상태 코드)은 [`api-conventions.md`](api-conventions.md)가 단일 기준이다.

| 항목 | 내용 |
|---|---|
| 상태 | 활성 |
| 작성일 | 2026-09-16 |
| 관련 이슈 | [#5](https://github.com/silversieon/likelion14th-festival-be-V2/issues/5) |
| 관련 LLD | [LLD-0002](../lld/LLD-0002-university-department-search-api.md) |
| 컨트롤러 | `domain/university/controller/UniversityController` |

로그인·회원가입 화면에서 **학교명 검색 → 학교 선택 → 학과 선택** 흐름으로 `universityId` / `departmentId`를 얻기 위한 API다 (`auth.md` 0장).

- **두 API 모두 인증이 필요 없다.** 로그인 전 화면에서 호출된다.
- 만료된 `ACCESS_TOKEN` 쿠키가 남아 있어도 **401이 나지 않는다** (JWT 필터를 거치지 않는다). 세션이 끊겨 로그인 화면으로 돌아온 사용자가 검색에 막히지 않게 하기 위함이다.

## 1. 학교명 검색

- **Method / Path**: `GET /api/universities`
- **인증**: 불필요 (permitAll, JWT 필터 제외)
- **멱등성 키**: 불필요
- **Swagger**: `@Tag("University")` / `@Operation(summary = "[ 사용자 | 토큰 X | 학교명 검색 ]")`

### 요청 — Query Parameter

| 파라미터 | 타입 | 필수 | 제약 | 설명 |
|---|---|---|---|---|
| name | string | Y | 공백만으로 이루어질 수 없음 | 검색할 학교명. **앞부분(왼쪽)부터 일치**하는 학교를 찾는다 |

- **전방 일치(prefix)** 검색이다. `name=서경` → `서경대학교` ✅ / `name=경대` → `서경대학교` ❌
- 앞뒤 공백은 제거한 뒤 검색한다.
- `%`, `_`는 와일드카드가 아니라 **문자 그대로** 검색한다.

```
GET /api/universities?name=서울
```

### 응답 — 200 OK

`data`는 배열이다. **학교명 오름차순**으로 정렬된다. 일치하는 학교가 없으면 빈 배열 `[]`이다.

| 필드 | 타입 | 설명 |
|---|---|---|
| [].universityId | number | 학교 식별자 |
| [].universityName | string | 학교명 |

```json
{
  "success": true,
  "code": 200,
  "message": "학교 검색에 성공했습니다.",
  "data": [
    { "universityId": 201, "universityName": "서울과학기술대학교" },
    { "universityId": 202, "universityName": "서울대학교" }
  ]
}
```

### 에러

| HTTP | code (내부) | message (실제 응답 문자열) | 발생 조건 |
|---|---|---|---|
| 400 | UNIVERSITY_40001 | 검색할 학교명을 입력해주세요. | `name`이 없거나 공백뿐 |

## 2. 학교별 학과 목록 조회

- **Method / Path**: `GET /api/universities/{universityId}/departments`
- **인증**: 불필요 (permitAll, JWT 필터 제외)
- **멱등성 키**: 불필요
- **Swagger**: `@Tag("University")` / `@Operation(summary = "[ 사용자 | 토큰 X | 학교별 학과 목록 조회 ]")`

### 요청 — Path Variable

| 변수 | 타입 | 필수 | 설명 |
|---|---|---|---|
| universityId | number | Y | 학교 식별자 (1번 API 응답의 `universityId`) |

```
GET /api/universities/202/departments
```

### 응답 — 200 OK

`data`는 배열이다. 해당 학교의 **학과 전체**를 **학과명 오름차순**으로 반환한다. 페이지네이션은 하지 않는다 (학교당 학과 수가 최대 수백 개 수준이다).

| 필드 | 타입 | 설명 |
|---|---|---|
| [].departmentId | number | 학과 식별자 |
| [].departmentName | string | 학과명 |

```json
{
  "success": true,
  "code": 200,
  "message": "학과 목록 조회에 성공했습니다.",
  "data": [
    { "departmentId": 5012, "departmentName": "경영학과" },
    { "departmentId": 5013, "departmentName": "소프트웨어학과" }
  ]
}
```

### 에러

| HTTP | code (내부) | message (실제 응답 문자열) | 발생 조건 |
|---|---|---|---|
| 400 | (공통 — 코드 없음) | 유효하지 않은 입력 요청 발생 | `universityId`가 숫자가 아님 (`GlobalExceptionHandler`) |
| 404 | UNIVERSITY_40401 | 해당 대학을 찾을 수 없습니다. | 존재하지 않는 `universityId` |

> 학과가 하나도 없는 학교는 404가 아니라 **빈 배열**이다. 학교 존재 여부와 학과 유무를 구분한다.
