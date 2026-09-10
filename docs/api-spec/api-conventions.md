# API 공통 규약 (API Conventions)

> **모든 REST API는 이 문서의 규약을 따른다.**
> 컨트롤러를 개발하기 전에 해당 도메인의 API 스펙 문서를 이 디렉터리에 먼저 작성하고(AGENTS.md 워크플로 ③),
> 구현은 스펙 문서와 일치해야 한다. 스펙과 다르게 구현해야 하면 스펙 문서를 먼저 수정한다.

| 항목 | 내용 |
|---|---|
| 상태 | 활성 |
| 작성일 | 2026-09-10 (기존 뱅킹 프로젝트 문서를 축제 서비스 현행 코드 기준으로 전면 재작성) |
| 기준 코드 | `global/common/BaseResponse`, `global/exception/GlobalExceptionHandler`, `global/config/SwaggerConfig`, 각 도메인 `controller` |
| 관련 ADR | 아직 없음 — 응답 형식·에러 코드 체계를 바꾸려면 ADR을 먼저 작성한다 |

## 1. 파일 구성 규칙

- 도메인별로 하나의 스펙 파일을 둔다: `docs/api-spec/<domain>.md`
  현재 대상: `auth.md`, `booth.md`, `menu.md`, `order.md`, `order-sse.md`, `lostitem.md`, `manager.md`
  (전국 확장 작업이 시작되면 `university.md`가 추가된다)
- 파일 내 각 API는 아래 "8. API 명세 템플릿" 형식으로 작성한다.
- API가 변경되면 스펙 파일을 같은 PR에서 함께 갱신한다. **스펙 문서가 곧 API의 단일 진실 공급원(single source of truth)이다.**

### 1.1 스펙 문서와 Swagger의 역할 분담

Swagger(자동 생성 문서)를 도입했지만 **스펙 문서를 대체하지 않는다.** 둘의 역할이 다르다.

| | `docs/api-spec/<domain>.md` | Swagger (`/v3/api-docs`) |
|---|---|---|
| 생성 시점 | **구현 전** (LLD 작성 시점) | **구현 후** (코드에서 자동 생성) |
| 성격 | 설계 계약 — "이렇게 만들기로 한다" | 구현 반영 — "실제로 이렇게 동작한다" |
| 권위 | **단일 진실 공급원** | 계약 준수 여부를 확인하는 검증 수단 |

- 둘이 다르면 **둘 중 하나가 잘못된 것**이다. 구현이 틀렸으면 구현을, 계약을 바꿔야 하면 **스펙 문서를 먼저 고치고** 구현을 따라가게 한다.
- 구현 전에는 Swagger가 존재하지 않으므로, Swagger가 있다고 해서 스펙 문서 작성을 생략하지 않는다.

## 2. URL / 메서드

### 2.1 Base path

- **모든 API의 base path는 `/api`다. 버전 세그먼트(`/v1`)는 사용하지 않는다.**
  현재 컨트롤러는 `@RequestMapping("/api")`를 클래스에 두고 메서드에서 자원 경로 전체를 적는 방식과,
  `@RequestMapping("/api/lost-items")`처럼 자원까지 클래스에 묶는 방식이 섞여 있다.
  **신규 컨트롤러는 후자(`/api/<자원>`을 클래스에 묶는 방식)를 쓴다.**
- 자원 이름은 **복수형 kebab-case**: `/api/booths`, `/api/booth-menus`, `/api/orders`, `/api/order-item-units`, `/api/lost-items`, `/api/managers`
- 경로 변수는 camelCase: `{boothId}`, `{menuId}`, `{orderId}`, `{lostItemId}`, `{orderItemUnitId}`

### 2.2 메서드 사용 규칙

| 메서드 | 용도 | 예 |
|---|---|---|
| `GET` | 조회 | `GET /api/booths`, `GET /api/orders/waiting` |
| `POST` | 생성 | `POST /api/booths/{boothId}/orders` |
| `PUT` | 전체 교체 | `PUT /api/booth-menus/{menuId}` |
| `PATCH` | 부분 변경·상태 전이 | `PATCH /api/orders/{orderId}/status` |
| `DELETE` | 삭제 | `DELETE /api/booth-menus/{menuId}` |

- 메서드로 표현 불가한 도메인 행위는 하위 경로 동사/명사를 허용한다 (`PATCH /api/orders/{orderId}/cancel`, `PATCH /api/booths/{boothId}/thumbnail`).
- **하위 자원 조회는 컬렉션 경로 아래 필터 경로로 표현한다**: `GET /api/orders/waiting|cooking|completed|canceled`.

> **알려진 예외 (신규 API에서 따라하지 않는다)**
> `PATCH /api/booths/status/open`, `PATCH /api/booths/status/close`는 대상 부스를 경로가 아니라
> **JWT의 학과(`departmentName`)로 결정**한다. 자원 식별자가 경로에 없는 형태이므로 규약 위반이지만,
> 프런트가 이미 사용 중이라 유지한다. 신규 API는 `/api/booths/{boothId}/status` 형태로 만든다.

## 3. 요청 형식

- Content-Type: `application/json`, 필드는 camelCase.
- **이미지가 포함되는 요청은 `multipart/form-data`**를 쓰고, JSON 본문은 `request` 파트, 파일은 별도 파트로 보낸다.
  (예: `POST /api/booths` — `@RequestPart("request") BoothRequest`, `@RequestPart("thumbnail") MultipartFile`)
- **금액은 JSON number(정수)**로 표현한다. DB 컬럼이 `INT`이며 KRW는 소수점이 없다. 예: `"menuPrice": 5000`
  `BigDecimal`/`double`을 요청·응답 DTO에 쓰지 않는다.
- 날짜/시각은 ISO-8601을 쓴다.
    - 날짜만: `2026-05-01` (`LocalDate`, `@DateTimeFormat(iso = ISO.DATE)`)
    - 날짜+시각: `2026-05-01T18:30:00` (`LocalDateTime`, 타임존 없음 — 서버 로컬 시각 기준)
- **enum은 이름 문자열(대문자)로 주고받는다.** 예: `"orderStatus": "COOKING"`, `"lang": "KO"`, `"location": "DAEIL"`
  (`Department`는 `@JsonCreator`로 이름 문자열을 파싱하며, 없는 값이면 `AuthErrorCode.INVALID_DEPARTMENT`가 난다)
- 검증은 `@Valid` + Bean Validation 애노테이션으로 DTO에서 수행한다. 컨트롤러 본문에서 if 검증을 하지 않는다.

## 4. 응답 형식 — 공통 envelope `BaseResponse` (전 API 공통, 개별 스펙에서 재정의 금지)

**성공·실패를 가리지 않고 모든 응답은 `global/common/BaseResponse`의 4개 필드 봉투(envelope)로 감싼다.**

| 필드 | 타입 | 설명 |
|---|---|---|
| `success` | boolean | 성공 여부. 성공 팩토리는 `true`, 실패 팩토리는 `false`로 고정 |
| `code` | number | **HTTP 상태 코드와 같은 정수값** (문자열 에러 코드가 아니다) |
| `message` | string | 성공/실패 사유를 담은 한국어 메시지 |
| `data` | object \| array \| null | 성공 시 응답 본문. 실패 시 항상 `null` |

```json
// 성공 — 201 Created
{
  "success": true,
  "code": 201,
  "message": "주문 요청에 성공했습니다.",
  "data": {
    "orderId": 1024,
    "customerName": "홍길동",
    "totalOrderPrice": 21000
  }
}
```

```json
// 실패 — 400 Bad Request
{
  "success": false,
  "code": 400,
  "message": "주문 총 가격이 메뉴 목록 총 가격과 일치하지 않습니다.",
  "data": null
}
```

**중요한 제약 — HTTP 상태 코드와 `code`는 항상 일치한다.**

- envelope은 상태 코드를 **대체하는 것이 아니라 덧붙이는 것**이다. 성공은 2xx, 비즈니스 규칙 위반은 4xx, 시스템 오류는 5xx.
- **`200 OK` + `"success": false` 같은 모순된 조합은 존재하지 않는다.** `success`는 `BaseResponse` 정적 팩토리만 결정하며 외부에서 지정할 수 없다.
- 클라이언트는 `success`로 분기해도 되고 HTTP 상태 코드로 분기해도 된다. 두 값은 항상 일치한다.

**상태 코드 사용 규칙**

| 상황 | 상태 코드 | 본문 |
|---|---|---|
| 조회·수정 성공 | `200 OK` | envelope + `data` |
| 생성 성공 | `201 Created` | envelope + `data` |
| 본문 없는 성공 (상태 변경, 삭제 등) | **`200 OK`** | envelope + `data: null` |
| 비즈니스 규칙 위반 | `4xx` (주로 400 / 403 / 404 / 409) | envelope + `message` |
| 시스템 오류 | `5xx` | envelope + `message` |

- **`204 No Content`는 사용하지 않는다.** 204만 본문이 없으면 클라이언트가 다시 분기해야 해서 envelope의 목적이 깨진다.
- `Location` 헤더는 현재 생성 API에서 사용하지 않는다. 필요해지면 규약을 먼저 갱신한다.

### 4.1 페이지네이션 — 커서 방식

목록 조회는 **커서 페이지네이션**(마지막으로 읽은 위치를 커서 문자열로 들고 다니며 다음 페이지를 읽는 방식 — offset/limit과 달리 뒤로 갈수록 느려지지 않는다)을 사용한다.

**요청 파라미터**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `encodedCursor` | string | N | (없음 = 첫 페이지) | 직전 응답의 `nextCursor`를 그대로 전달 |
| `size` | number | N | `20` | 페이지 크기 |

**응답 `data` 구조 (`CursorPageResponse<T>`)**

| 필드 | 타입 | 설명 |
|---|---|---|
| `items` | array | 현재 페이지 항목 |
| `nextCursor` | string \| null | 다음 요청에 그대로 넣을 커서 (`CursorCodec`가 Base64로 인코딩) |
| `hasNext` | boolean | 다음 페이지 존재 여부 |
| `size` | number | 요청된 페이지 크기 |

- 커서의 내부 구조(정렬 키 조합)는 도메인별 `*Cursor` 레코드가 정의하며 **클라이언트에게 불투명한 문자열이다.** 클라이언트가 파싱·생성하지 않는다.
- 새 목록 API를 만들 때는 커서에 쓰는 정렬 키를 LLD에 명시하고, 그 키 조합에 맞는 인덱스를 함께 설계한다.

## 5. 에러 정책

### 5.1 예외 처리 흐름

```
Service/Domain → throw new CustomException(XxxErrorCode.YYY)
                    ↓
GlobalExceptionHandler (@RestControllerAdvice)
                    ↓
ResponseEntity.status(errorCode.getStatus())
    .body(BaseResponse.error(errorCode.getStatus().value(), errorCode.getMessage()))
```

- 실패는 **예외를 던지는 것으로 끝낸다.** 컨트롤러에서 try-catch 하지 않는다.
- 모든 에러 코드 enum은 `global/exception/model/BaseErrorCode`를 구현한다 (`getCode()`, `getMessage()`, `getStatus()`).
- 도메인 에러 코드는 해당 도메인의 `exception/<Domain>ErrorCode.java`에 둔다. 공통은 `global/exception/GlobalErrorCode`.

### 5.2 ⚠️ 에러 코드 문자열은 현재 응답 본문에 내려가지 않는다 (알려진 갭)

`BaseResponse.error(int code, String message)`는 **HTTP 상태값과 메시지만** 담는다.
`BaseErrorCode.getCode()`(`ORDER_4001`, `G001` 등)는 **로그에만 남고 클라이언트에게 전달되지 않는다.**

- 따라서 **클라이언트는 현재 `message` 문자열로만 상황을 구분할 수 있다.** 스펙 문서의 에러 표에는 코드와 함께 **정확한 메시지 문자열**을 반드시 적는다.
- 이 구조를 바꾸려면(예: `errorCode` 필드 추가) **`BaseResponse` 스키마 변경이므로 ADR을 먼저 작성한다.** 프런트엔드 계약이 깨지는 변경이다.

### 5.3 에러 코드 명명 규칙 (신규 코드부터 적용)

`<도메인 대문자>_<HTTP 상태 3자리><일련 2자리>` — 예: `ORDER_40001`, `BOOTH_40401`

- 상태 코드를 코드 안에 넣어 코드만 보고 HTTP 상태를 알 수 있게 한다.
- 번호는 **재사용하지 않는다.** 폐기된 코드의 번호를 비워 둔다.
- 도메인 접두사: `ORDER`, `BOOTH`, `MENU`, `LOST_ITEM`, `MANAGER`, `AUTH`, `S3`, `I`(멱등성), `G`(공통)

> **기존 코드는 이 규칙과 어긋난다.** `OrderErrorCode`에는 `ORDER_4001`(언더바 있음)과 `ORDER4031`(없음)이 섞여 있고,
> **`ORDER_4007`이 `NOT_TIME_TO_ORDER`와 `ORDER_MENU_SOLD_OUT` 두 곳에 중복**되어 있다.
> 에러 코드가 응답에 나가지 않아 당장 장애는 없지만, 5.2를 해소하는 작업과 함께 정리 대상이다. 별도 이슈로 다룬다.

### 5.4 상태 코드 매핑 원칙

| 상황 | 상태 코드 |
|---|---|
| 요청 값 검증 실패 (`@Valid`, 타입 불일치, 파싱 실패) | `400` |
| 인증 실패 (토큰 없음/만료/위조) | `401` |
| 권한 부족 (`@PreAuthorize` 거부, 타 부스 자원 접근) | `403` |
| 자원 없음 | `404` |
| 상태 충돌 (이미 처리 중, 되돌릴 수 없는 전이) | `409` |
| 만료된 멱등성 키 | `410` |
| 업로드 용량 초과 | `413` |
| 시스템 오류 | `5xx` |

- **5xx 응답의 `message`에는 내부 구조가 드러나는 상세를 넣지 않는다.** 상세는 로그에만 남긴다.
  (`GlobalExceptionHandler`의 최종 `Exception` 핸들러는 항상 `"서버 내부 오류가 발생했습니다."`만 내려준다)
- SSE 연결 관련 예외(`AsyncRequestNotUsableException`, `AsyncRequestTimeoutException`)는 **응답 본문 없이 로그만 남긴다.** 클라이언트 연결 종료는 정상 흐름이다.

## 6. 공통 헤더

| 헤더 | 방향 | 필수 | 설명 |
|---|---|---|---|
| `Authorization: Bearer <JWT>` | 요청 | 인증 필요 API | Access Token. **또는 `ACCESS_TOKEN` 쿠키로도 받는다** (`JwtProvider.extractAccessToken`이 헤더 → 쿠키 순으로 조회) |
| `Cookie: ACCESS_TOKEN=<JWT>` | 요청 | 위와 택일 | 브라우저 클라이언트가 사용하는 경로 |
| `Cookie: REFRESH_TOKEN=<JWT>` | 요청 | `/api/auth/refresh`, `/api/auth/logout` | Refresh Token은 **쿠키로만** 받는다 |
| `Set-Cookie` | 응답 | 로그인/재발급/로그아웃 | `/api/auth/*`가 두 토큰 쿠키를 심거나 만료시킨다. 본문의 `data`는 `null`이다 |
| `Idempotency-Key: <UUID>` | 요청 | **주문 생성 API 필수** | 누락 시 400 (`@RequestHeader` 필수) |

- **`traceId`는 응답으로 내려가지 않는다.** `MdcFilter`가 요청마다 8자리 ID를 만들어 **로그 MDC에만** 넣는다.
  응답에 추적 ID를 노출하려면 `BaseResponse` 또는 응답 헤더 규약을 바꿔야 하므로 ADR 대상이다 (5.2와 함께 다룬다).

### 6.1 인증 없이 접근 가능한 API (`SecurityConfig` 기준)

| 경로 |
|---|
| `POST /api/auth/**` (register, login, refresh, logout) |
| `GET /api/booths`, `/api/booths/search`, `/api/booths/{boothId}`, `/api/booths/{boothId}/account` |
| `GET /api/booths/{boothId}/menus/order-available` |
| `GET /api/lost-items`, `/api/lost-items/{lostItemId}` |
| **`POST /api/booths/{boothId}/orders`** (QR 오더 — 손님은 로그인하지 않는다) |
| `/swagger-ui/**`, `/v3/api-docs/**`, `/error`, `/actuator/health` |

그 외 모든 경로는 인증이 필요하며, 권한은 메서드에 `@PreAuthorize`로 건다.

### 6.2 역할(Role)

`domain/manager/entity/enums/Role`: `USER`, `ADMIN`, `BOOTH_MANAGER`, `STUDENT_COUNCIL`

- JWT의 subject는 **`Department` enum 이름**이며(`SOFTWARE`, `SKULIKELION` …), 컨트롤러에서 `@AuthenticationPrincipal String departmentName`으로 받는다.
- 즉 **"어느 부스의 데이터인가"는 경로가 아니라 토큰이 결정하는 API가 많다.** 스펙 문서에는 그 사실을 `인증` 항목에 반드시 명시한다.

## 7. 멱등성 (주문 생성 API)

`global/util/idempotency`의 `@Idempotent` AOP가 처리한다.

```java
@Idempotent(idempotencyKey = "#idempotencyKey", strategy = IdempotencyType.FALLBACK)
public OrderResponse createOrder(Long boothId, String idempotencyKey, OrderCreateRequest request)
```

| 항목 | 내용 |
|---|---|
| 키 전달 | `Idempotency-Key` 요청 헤더 (UUID 문자열) |
| 키 저장 | `idempotency` 테이블 (`idempotency_key BINARY(16)` PK) 또는 Redis — 전략에 따름 |
| 전략 | `DB`, `REDIS`, `WRITETHROUGH`, `FALLBACK` (`IdempotencyType`). **주문 생성은 `FALLBACK`** |
| 응답 재사용 | `response_body JSON` 컬럼에 최초 응답을 저장했다가 재요청 시 그대로 반환 |
| 상태 | `status` 컬럼: 처리 중 / 완료 (`IdempotencyStatus`) |

| 상황 | 응답 |
|---|---|
| 완료된 키 재요청 | 최초 응답과 동일한 본문·상태 반환 (재처리하지 않음) |
| 처리 중인 키 재요청 | `409` — `"이미 처리 중인 주문입니다."` (`ORDER_ALREADY_PROCESSING`) |
| 만료된 키 재요청 | `410` — `"멱등성 키가 만료되었습니다. 새로운 요청을 보내주세요."` (`ORDER_IDEMPOTENCY_KEY_EXPIRED`) |

- **키 만료·정리는 `DbIdempotencyScheduler`가 담당한다.** 보관 기간을 바꾸는 것은 스펙 변경이므로 문서에 반영한다.
- 새로 멱등성이 필요한 API를 만들면 **어떤 전략을 쓰는지, 왜 그 전략인지**를 LLD와 스펙 문서에 남긴다.

## 8. SSE (Server-Sent Events) 규약

주문 관리 화면은 폴링 대신 SSE로 실시간 갱신한다.

| 항목 | 내용 |
|---|---|
| 구독 | `GET /api/orders/subscribe?orderSseSubscribeType=WAITING` — `produces = text/event-stream` |
| 권한 | `BOOTH_MANAGER` |
| 구독 타입 | `WAITING`, `COOKING`, `COMPLETED`, `CANCELED`, `PRODUCT` (`OrderSseSubscribeType`) |
| 연결 확인 이벤트 | name `connect`, data `connected order subscribe` |

**이벤트 이름 (`OrderSseEventType` — 이 목록이 계약이다)**

| 이벤트 이름 | 발생 시점 |
|---|---|
| `waitingOrderEvent` | 주문 생성 |
| `cookingOrderEvent` | 조리 중으로 전이 |
| `completedOrderEvent` | 완료로 전이 |
| `canceledOrderEvent` | 주문 취소 |
| `orderItemUnitStatusEvent` | 개별 상품 서빙 여부 변경 |
| `orderIncrementNotification` | 해당 탭의 주문 건수 증가 |
| `orderDecrementNotification` | 해당 탭의 주문 건수 감소 |
| `dismissNotification` | 알림 해제 |

- SSE 이벤트의 `data`는 **`BaseResponse`로 감싸지 않는다.** 페이로드 JSON을 그대로 보낸다 (envelope은 HTTP 응답 규약이다).
- 이벤트를 추가·변경하면 `docs/api-spec/order-sse.md`와 `OrderSseEventType`을 **같은 PR에서** 함께 고친다. 프런트가 이름 문자열로 구독하므로 오타 하나가 곧 장애다.
- 전달 방식(`local` / Redis `pubsub` / Redis `stream`)은 서버 내부 구현이며 **클라이언트 계약에 영향을 주지 않는다.** 스펙 문서에 적지 않는다.

## 9. 컨트롤러 구현 규약

```java
@Tag(name = "Order", description = "사용자 주문 관련 기능을 제공하는 API")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api")
public class OrderController {

  private final OrderService orderService;

  @Operation(summary = "[ 사용자 | 토큰 X | 주문 생성 요청 ]", description = """
      **Parameters** \n
      Idempotency-Key: 멱등성 키 \n
      ...
      **Returns** \n
      orderId: 주문 식별자 \n
      """)
  @PostMapping("/booths/{boothId}/orders")
  public ResponseEntity<BaseResponse<OrderResponse>> createOrder(
      @PathVariable Long boothId,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody OrderCreateRequest request) {
    OrderResponse response = orderService.createOrder(boothId, idempotencyKey, request);
    return ResponseEntity.status(201)
        .body(BaseResponse.success(201, "주문 요청에 성공했습니다.", response));
  }
}
```

- **반환 타입은 항상 `ResponseEntity<BaseResponse<T>>`다.** 평문 DTO를 직접 반환하지 않는다. 본문이 없으면 `BaseResponse<Void>` + `data`에 `null`.
- 팩토리는 **`BaseResponse.success(code, message, data)` 3인자 형태를 기본**으로 쓴다.
  (1인자 `success(data)`·2인자 `success(message, data)`는 200 고정이라 생성 API에서 쓸 수 없다. 형태를 통일해 둔다)
- `ResponseEntity.status(...)`의 상태값과 `BaseResponse.success(...)`의 첫 인자를 **반드시 같은 값**으로 준다. 둘이 어긋나면 클라이언트 분기가 깨진다.
- **감싸는 코드를 컨트롤러에 명시적으로 둔다.** `ResponseBodyAdvice` 등으로 자동 래핑하지 않는다 — 코드만 보고 실제 응답 모양을 알 수 있어야 하고, Swagger 스키마가 실제 응답과 일치해야 하기 때문이다.
- **`message`는 한국어 서술문**이며, 성공 메시지는 `"<행위>에 성공했습니다."` 형태를 기본으로 한다.
- `BaseResponse`는 컨트롤러(및 `SecurityConfig`·필터의 직접 응답 작성) 전용이다. **service·repository 시그니처에 등장하면 안 된다.**
- 권한은 `@PreAuthorize`로 메서드에 건다. 서비스 안에서 역할을 if로 검사하지 않는다.

## 10. API 명세 템플릿

각 도메인 스펙 파일에서 API 하나당 아래 형식으로 작성한다.
**응답 표에는 envelope의 `data` 안쪽 필드만 기술한다** — envelope 4개 필드는 4장에 정의되어 있으므로 API마다 반복하지 않는다.

```markdown
## <기능 이름> (예: 주문 생성)

- **Method / Path**: `POST /api/booths/{boothId}/orders`
- **인증**: 불필요 (permitAll)
  <!-- 또는: 필요 (BOOTH_MANAGER) — 대상 부스는 JWT의 department로 결정 -->
- **멱등성 키**: 필수 (`Idempotency-Key`, 전략 FALLBACK) | 불필요
- **Swagger**: `@Tag("Order")` / `@Operation(summary = "[ 사용자 | 토큰 X | 주문 생성 요청 ]")`
- **관련 문서**: LLD-NNNN, 이슈 #N

### 요청

| 필드 | 타입 | 필수 | 제약 | 설명 |
|---|---|---|---|---|
| tableNumber | number | Y | > 0 | 테이블 번호 |
| orderItems[].boothMenuId | number | Y | | 부스 메뉴 식별자 |

### 응답 — 201 Created

`data` 필드의 구조 (envelope은 api-conventions 4장 참조):

| 필드 | 타입 | 설명 |
|---|---|---|
| orderId | number | 생성된 주문 식별자 |

### 발행 이벤트 (SSE, 해당 시)

| 이벤트 이름 | 수신 대상 | 페이로드 |
|---|---|---|
| waitingOrderEvent | 해당 부스 WAITING 구독자 | WaitingOrderResponse |

### 에러

| HTTP | code (내부) | message (실제 응답 문자열) | 발생 조건 |
|---|---|---|---|
| 400 | ORDER_4001 | 주문 총 가격이 메뉴 목록 총 가격과 일치하지 않습니다. | 합계 불일치 |
| 409 | ORDER4091 | 이미 처리 중인 주문입니다. | 동일 멱등성 키가 처리 중 |
```

## 11. Swagger(springdoc-openapi) 애노테이션 규약

브라우저에서 API 목록·스키마를 확인하고 직접 호출해볼 수 있도록, **컨트롤러와 DTO에 문서화 애노테이션을 붙인다.**

### 11.1 접근 경로

| 경로 | 용도 |
|---|---|
| `/swagger-ui/index.html` | Swagger UI |
| `/v3/api-docs` | OpenAPI JSON |

둘 다 `permitAll`이다.

### 11.2 애노테이션 사용 표

| 대상 | 애노테이션 | 필수 |
|---|---|---|
| 컨트롤러 클래스 | `@Tag(name = "Order", description = "사용자 주문 관련 기능을 제공하는 API")` | ✅ |
| 컨트롤러 메서드 | `@Operation(summary = "...", description = "...")` | ✅ |
| 요청/응답 DTO 필드 | `@Schema(description = "초기 주문 금액", example = "5000")` | ✅ |
| enum | `@Schema(description = "주문 상태")` | ✅ |
| 경로/쿼리 변수 | `@Parameter(description = "부스 식별자", example = "1")` | 권장 |
| 고유 에러 응답 | `@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "...")` | 해당 시 |

**`@Operation(summary)` 작성 규칙 (기존 관례 — 반드시 따른다)**

```
[ <대상> | 토큰 O|X | <기능> ]
```

예: `[ 부스 관리자 | 토큰 O | 대기 중 주문 목록 조회 ]`, `[ 사용자 | 토큰 X | 주문 생성 요청 ]`
`<대상>`은 `사용자`, `부스 관리자`, `총학생회`, `관리자` 중 하나를 쓴다.

**`@Operation(description)` 작성 규칙**

기존 컨트롤러는 텍스트 블록에 `**Parameters**` / `**Returns**` 절을 나열하는 형식을 쓴다. 신규 API도 같은 형식을 유지한다.
SSE 이벤트를 발행하는 API는 `EVENT NAME` / `EVENT DATA`를 함께 적는다.

### 11.3 이름 충돌 주의

우리 envelope은 `BaseResponse`이므로 springdoc의 `io.swagger.v3.oas.annotations.responses.ApiResponse`와 **단순 이름이 겹치지 않는다.** 그대로 import해서 쓰면 된다.

대신 **Jackson 패키지를 주의한다.** 이 프로젝트는 Jackson 3.x를 쓰므로 `ObjectMapper`는 **`tools.jackson.databind.ObjectMapper`**다.
`com.fasterxml.jackson.databind.ObjectMapper`를 import하면 빈 주입이 안 된다.
(`@JsonCreator` 같은 일부 애노테이션은 여전히 `com.fasterxml.jackson.annotation`에 있으므로 사례별로 확인한다.)

### 11.4 전역 설정

문서 제목·버전·서버·보안 스키마는 `global/config/SwaggerConfig`에서 한 번만 정의한다. 개별 컨트롤러가 재정의하지 않는다.

### 11.5 스펙 문서와의 대조 (PR 체크)

- [ ] 컨트롤러의 경로·메서드·상태 코드가 `docs/api-spec/<domain>.md`와 일치하는가
- [ ] `@Operation(summary)`가 스펙 문서의 기능 이름과 일치하는가
- [ ] 스펙 문서 에러 표의 **message 문자열**이 실제 `ErrorCode`의 메시지와 글자 단위로 같은가
- [ ] `ResponseEntity.status(N)`과 `BaseResponse.success(N, ...)`의 N이 같은가
- [ ] SSE 이벤트를 발행하는 API라면 이벤트 이름이 `OrderSseEventType`과 일치하는가

## 12. 에러 코드 등록 절차

1. 새 에러 상황이 생기면 해당 도메인의 `<Domain>ErrorCode` enum에 5.3 명명 규칙으로 추가한다 (번호 재사용 금지, **중복 번호 금지**).
2. 같은 PR에서 해당 도메인 스펙 파일의 "에러" 표를 갱신한다 — **code와 message 문자열을 모두** 적는다.
3. 컨트롤러의 Swagger 응답 설명에도 노출한다.
4. 5.2 때문에 클라이언트는 message로만 구분할 수 있으므로, **기존 message 문자열을 바꾸는 것은 계약 변경**이다. 프런트와 합의 없이 고치지 않는다.
