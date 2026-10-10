# API 스펙 — statistics (운영진 통계)

> 공통 규약(응답 envelope `BaseResponse`, 에러 정책, 헤더, 상태 코드)은 [`api-conventions.md`](api-conventions.md)가 단일 기준이다.

| 항목 | 내용 |
|---|---|
| 상태 | 활성 |
| 작성일 | 2026-10-10 |
| 관련 이슈 | [#16](https://github.com/silversieon/likelion14th-festival-be-V2/issues/16) |
| 관련 LLD | [LLD-0005](../lld/LLD-0005-admin-statistics-api.md) |
| 컨트롤러 | `domain/statistics/controller/StatisticsController` |

운영진 통계 화면(프런트 `/admin` — 매출 / 부스 / 메뉴 탭)에 쓰는 **조회 전용** API 7종이다.

## 0. 공통 규칙

- **인증**: 필요. **`ADMIN` 역할만** 호출할 수 있다 (`@PreAuthorize("hasRole('ADMIN')")`). 그 외 역할은 `403`, 토큰이 없으면 `401`.
- **멱등성 키**: 불필요 (조회).
- **집계 대상은 완료(`COMPLETED`)된 주문뿐이다.** 대기·조리 중·취소된 주문은 어떤 수치에도 포함되지 않는다.
- **기준 시각은 주문 완료 시각(`completedAt`)이다.** "10월 10일 매출"은 10월 10일에 **완료된** 주문의 합이다. 기존 부스 매출 조회(`GET /api/orders/sales`)와 같은 규칙이다.
- 금액은 원(KRW) 정수, 건수·수량은 정수다.
- 날짜 파라미터는 ISO-8601 `yyyy-MM-dd`이며, 형식이 틀리면 `400 유효하지 않은 입력 요청 발생`이다.
- 학교명·부스명 검색은 **부분 일치**(검색어가 이름 어디에든 포함되면 일치)다. 앞뒤 공백은 제거하고, `%`·`_`는 와일드카드가 아니라 문자 그대로 검색한다.
- "부스명"은 부스를 운영하는 **학과명**(`departmentName`)이다.

| # | Method / Path | 설명 |
|---|---|---|
| 1 | `GET /api/statistics/sales/summary` | 총 주문 수·총 수익 |
| 2 | `GET /api/statistics/sales/daily` | 일별 매출 추이 |
| 3 | `GET /api/statistics/sales/hourly` | 시간대별 매출 추이 |
| 4 | `GET /api/statistics/booths/top` | 부스별 매출 TOP 10 |
| 5 | `GET /api/statistics/booths/search` | 부스별 매출 검색 |
| 6 | `GET /api/statistics/booths/menus` | 부스별 인기 메뉴 순위 |
| 7 | `GET /api/statistics/menus/top` | 가장 많이 팔린 메뉴 TOP 10 |

### 공통 에러

| HTTP | code (내부) | message (실제 응답 문자열) | 발생 조건 |
|---|---|---|---|
| 401 | (인증 필터) | — | 토큰 없음·만료 |
| 403 | (공통 — 코드 없음) | 권한 없는 요청 발생 | `ADMIN`이 아닌 역할 |

## 1. 총 주문 수·총 수익

- **Method / Path**: `GET /api/statistics/sales/summary`
- **Swagger**: `@Tag("Statistics")` / `@Operation(summary = "[ 운영진 | 토큰 O | 총 주문 수·총 수익 조회 ]")`

### 요청

파라미터 없음.

### 응답 — 200 OK

| 필드 | 타입 | 설명 |
|---|---|---|
| totalOrders | number | 완료된 주문 수 (전체 기간) |
| totalSales | number | 완료된 주문의 총 주문 금액 합 (전체 기간). 주문이 없으면 `0` |

```json
{
  "success": true,
  "code": 200,
  "message": "총 매출 조회에 성공했습니다.",
  "data": { "totalOrders": 4810, "totalSales": 31245000 }
}
```

## 2. 일별 매출 추이

- **Method / Path**: `GET /api/statistics/sales/daily`
- **Swagger**: `@Operation(summary = "[ 운영진 | 토큰 O | 일별 매출 추이 조회 ]")`

### 요청 — Query Parameter

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| startDate | string (`yyyy-MM-dd`) | Y | 조회 시작일 (포함) |
| endDate | string (`yyyy-MM-dd`) | Y | 조회 종료일 (포함) |

```
GET /api/statistics/sales/daily?startDate=2026-10-04&endDate=2026-10-10
```

### 응답 — 200 OK

`data`는 배열이다. **시작일부터 종료일까지 모든 날짜**를 날짜 오름차순으로 반환한다. 주문이 없는 날도 `0`으로 채워 빠지지 않는다.

| 필드 | 타입 | 설명 |
|---|---|---|
| [].date | string (`yyyy-MM-dd`) | 일자 |
| [].totalSales | number | 그날 완료된 주문의 총 주문 금액 |
| [].orderCount | number | 그날 완료된 주문 수 |

```json
{
  "success": true,
  "code": 200,
  "message": "일별 매출 조회에 성공했습니다.",
  "data": [
    { "date": "2026-10-09", "totalSales": 1520000, "orderCount": 231 },
    { "date": "2026-10-10", "totalSales": 0, "orderCount": 0 }
  ]
}
```

### 에러

| HTTP | code (내부) | message | 발생 조건 |
|---|---|---|---|
| 400 | STATISTICS_40001 | 조회 시작일과 종료일을 모두 입력해주세요. | `startDate` 또는 `endDate`가 없음 |
| 400 | STATISTICS_40002 | 시작일은 종료일보다 늦을 수 없습니다. | `startDate` > `endDate` |

## 3. 시간대별 매출 추이

- **Method / Path**: `GET /api/statistics/sales/hourly`
- **Swagger**: `@Operation(summary = "[ 운영진 | 토큰 O | 시간대별 매출 추이 조회 ]")`

### 요청 — Query Parameter

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| startDate | string (`yyyy-MM-dd`) | N | 조회 시작일 (포함) |
| endDate | string (`yyyy-MM-dd`) | N | 조회 종료일 (포함) |

- **둘 다 생략 → 전체 기간** 합산 (화면의 "전체")
- **둘 다 지정 → 해당 기간** 합산 (화면의 "기간")
- 하나만 지정하면 400이다.

```
GET /api/statistics/sales/hourly
GET /api/statistics/sales/hourly?startDate=2026-10-09&endDate=2026-10-10
```

### 응답 — 200 OK

`data`는 배열이다. **11시부터 23시까지 13개 구간을 항상** 시각 오름차순으로 반환한다. `hour = 11`은 11:00:00 ~ 11:59:59에 완료된 주문이며, 마지막 `hour = 23`은 23:00 ~ 24:00이다. 주문이 없는 구간은 `0`이다.

- 11시 이전(00:00 ~ 10:59)에 완료된 주문은 이 API에 포함되지 않는다.

| 필드 | 타입 | 설명 |
|---|---|---|
| [].hour | number | 시각 (11 ~ 23) |
| [].totalSales | number | 해당 시각 구간의 총 주문 금액 (기간 내 모든 날짜 합산) |
| [].orderCount | number | 해당 시각 구간의 주문 수 (기간 내 모든 날짜 합산) |

```json
{
  "success": true,
  "code": 200,
  "message": "시간대별 매출 조회에 성공했습니다.",
  "data": [
    { "hour": 11, "totalSales": 120000, "orderCount": 18 },
    { "hour": 12, "totalSales": 0, "orderCount": 0 },
    { "hour": 23, "totalSales": 54000, "orderCount": 9 }
  ]
}
```

(예시는 일부 구간만 표기)

### 에러

| HTTP | code (내부) | message | 발생 조건 |
|---|---|---|---|
| 400 | STATISTICS_40001 | 조회 시작일과 종료일을 모두 입력해주세요. | 둘 중 하나만 지정 |
| 400 | STATISTICS_40002 | 시작일은 종료일보다 늦을 수 없습니다. | `startDate` > `endDate` |

## 4. 부스별 매출 TOP 10

- **Method / Path**: `GET /api/statistics/booths/top`
- **Swagger**: `@Operation(summary = "[ 운영진 | 토큰 O | 부스별 매출 TOP 10 조회 ]")`

### 요청

파라미터 없음.

### 응답 — 200 OK

`data`는 배열이다. **완료된 주문이 1건 이상 있는 부스** 중 총 매출 상위 10개를 순위 오름차순으로 반환한다. 해당 부스가 10개 미만이면 그만큼만 반환한다.

| 필드 | 타입 | 설명 |
|---|---|---|
| [].rank | number | 전체 부스 기준 매출 순위 (1부터) |
| [].boothId | number | 부스 식별자 |
| [].universityName | string | 학교명 |
| [].departmentName | string | 학과명 (부스명) |
| [].totalSales | number | 총 매출 |
| [].averageOrderAmount | number | 평균 주문 금액 = 총 매출 ÷ 총 주문 건수, **반올림**. 주문이 없으면 `0` |
| [].orderCount | number | 총 주문 건수 |
| [].totalQuantity | number | 총 판매 메뉴 수 (판매된 메뉴 수량의 합) |

**순위 규칙**: 총 매출 내림차순, 같으면 `boothId` 오름차순. 동점이어도 순위가 겹치지 않는다(1, 2, 3 …).

```json
{
  "success": true,
  "code": 200,
  "message": "부스별 매출 TOP 10 조회에 성공했습니다.",
  "data": [
    {
      "rank": 1,
      "boothId": 1,
      "universityName": "서경대학교",
      "departmentName": "소프트웨어학과",
      "totalSales": 8603000,
      "averageOrderAmount": 8436,
      "orderCount": 1020,
      "totalQuantity": 1931
    }
  ]
}
```

## 5. 부스별 매출 검색

- **Method / Path**: `GET /api/statistics/booths/search`
- **Swagger**: `@Operation(summary = "[ 운영진 | 토큰 O | 부스별 매출 검색 ]")`

### 요청 — Query Parameter

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| universityName | string | Y | 학교명 (부분 일치) |
| boothName | string | N | 부스(학과)명 (부분 일치). 생략·공백이면 학교 조건만 적용 |

- 학교명만 → **그 학교의 모든 부스** (주문이 없는 부스도 `0`으로 포함)
- 학교명 + 부스명 → 그 학교에서 부스명이 일치하는 부스만

```
GET /api/statistics/booths/search?universityName=서경대학교
GET /api/statistics/booths/search?universityName=서경대학교&boothName=소프트웨어학과
```

### 응답 — 200 OK

4번과 **필드가 같다.** 순위 오름차순으로 정렬된다. 일치하는 부스가 없으면 빈 배열 `[]`이다.

- **`rank`는 검색 결과 안의 순위가 아니라 전체 부스 기준 순위다.** 서경대학교만 검색해도 1위가 아니라 전국 순위(예: 37위)가 나온다.
- 주문이 없는 부스는 매출 0인 부스들끼리 `boothId` 오름차순으로 맨 뒤 순위를 받는다.

```json
{
  "success": true,
  "code": 200,
  "message": "부스별 매출 검색에 성공했습니다.",
  "data": [
    {
      "rank": 37,
      "boothId": 1,
      "universityName": "서경대학교",
      "departmentName": "소프트웨어학과",
      "totalSales": 8603000,
      "averageOrderAmount": 8436,
      "orderCount": 1020,
      "totalQuantity": 1931
    }
  ]
}
```

### 에러

| HTTP | code (내부) | message | 발생 조건 |
|---|---|---|---|
| 400 | STATISTICS_40003 | 검색할 학교명을 입력해주세요. | `universityName`이 없거나 공백뿐 |

## 6. 부스별 인기 메뉴 순위

- **Method / Path**: `GET /api/statistics/booths/menus`
- **Swagger**: `@Operation(summary = "[ 운영진 | 토큰 O | 부스별 인기 메뉴 순위 조회 ]")`

### 요청 — Query Parameter

5번과 같다 (`universityName` 필수, `boothName` 선택).

```
GET /api/statistics/booths/menus?universityName=서경대학교&boothName=소프트웨어학과
```

### 응답 — 200 OK

`data`는 배열이다. 검색된 **각 부스의 모든 메뉴**를 부스 안에서의 순위와 함께 반환한다. 팔리지 않은 메뉴도 `0`으로 포함된다.

- 정렬: 학교명 → 학과명 → `boothId` 오름차순으로 부스를 묶고, 부스 안에서는 순위 오름차순
- **순위 규칙(부스 안)**: 주문 수 내림차순 → 총 수익 내림차순 → `boothMenuId` 오름차순. 부스마다 1위부터 다시 매긴다.

| 필드 | 타입 | 설명 |
|---|---|---|
| [].rank | number | 부스 안에서의 순위 (1부터) |
| [].boothId | number | 부스 식별자 |
| [].boothMenuId | number | 메뉴 식별자 |
| [].universityName | string | 학교명 |
| [].departmentName | string | 학과명 (부스명) |
| [].menuName | string | 메뉴명 (한국어) |
| [].quantity | number | 주문 수 (판매 수량의 합) |
| [].sales | number | 총 수익 |

```json
{
  "success": true,
  "code": 200,
  "message": "부스별 인기 메뉴 순위 조회에 성공했습니다.",
  "data": [
    {
      "rank": 1,
      "boothId": 1,
      "boothMenuId": 11,
      "universityName": "서경대학교",
      "departmentName": "소프트웨어학과",
      "menuName": "닭꼬치",
      "quantity": 612,
      "sales": 2448000
    }
  ]
}
```

### 에러

| HTTP | code (내부) | message | 발생 조건 |
|---|---|---|---|
| 400 | STATISTICS_40003 | 검색할 학교명을 입력해주세요. | `universityName`이 없거나 공백뿐 |

## 7. 가장 많이 팔린 메뉴 TOP 10

- **Method / Path**: `GET /api/statistics/menus/top`
- **Swagger**: `@Operation(summary = "[ 운영진 | 토큰 O | 메뉴 TOP 10 조회 ]")`

### 요청 — Query Parameter

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| sortType | string | N | `QUANTITY`(판매순, 기본값) \| `SALES`(수익순) |

```
GET /api/statistics/menus/top?sortType=SALES
```

### 응답 — 200 OK

`data`는 배열이다. **한 번이라도 팔린 메뉴** 중 전국 상위 10개를 순위 오름차순으로 반환한다.

- `QUANTITY`: 판매량 내림차순 → 총 수익 내림차순 → `boothMenuId` 오름차순
- `SALES`: 총 수익 내림차순 → 판매량 내림차순 → `boothMenuId` 오름차순

| 필드 | 타입 | 설명 |
|---|---|---|
| [].rank | number | 순위 (1부터) |
| [].boothId | number | 부스 식별자 |
| [].boothMenuId | number | 메뉴 식별자 |
| [].universityName | string | 학교명 |
| [].departmentName | string | 학과명 (부스명) |
| [].menuName | string | 메뉴명 (한국어) |
| [].quantity | number | 판매량 |
| [].sales | number | 총 수익 |

```json
{
  "success": true,
  "code": 200,
  "message": "메뉴 TOP 10 조회에 성공했습니다.",
  "data": [
    {
      "rank": 1,
      "boothId": 1,
      "boothMenuId": 11,
      "universityName": "서경대학교",
      "departmentName": "소프트웨어학과",
      "menuName": "닭꼬치",
      "quantity": 612,
      "sales": 2448000
    }
  ]
}
```

### 에러

| HTTP | code (내부) | message | 발생 조건 |
|---|---|---|---|
| 400 | (공통 — 코드 없음) | 유효하지 않은 입력 요청 발생 | `sortType`이 `QUANTITY`/`SALES`가 아님 |
