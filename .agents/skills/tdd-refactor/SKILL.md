---
name: tdd-refactor
description: |
  tdd-green 단계로 모든 테스트가 통과한 상태에서, 테스트를 안전망 삼아 코드 품질을 개선하는 스킬.
  중복 제거, 책임 분리, 매직 스트링·매직 넘버 정리, 가독성 향상 등을 수행한다.
  외부 동작은 변경하지 않으며, 모든 리팩터링 후에도 기존 테스트가 그대로 통과해야 한다.
  "tdd refactor 해줘", "리팩터링 해줘", "코드 정리해줘", "이 코드 깔끔하게 해줘",
  "refactor 단계 진행해줘", "구조 개선해줘", "중복 제거해줘" 등의 표현이 나오면 반드시 이 스킬을 사용할 것.
  Java + Spring Boot + JUnit5 + Mockito + AssertJ 환경을 기본으로 한다.
---

# TDD Refactor Skill

tdd-green 단계로 통과시킨 코드를 **외부 동작은 유지한 채 내부 구조만 개선**한다.

> **Refactor 단계의 핵심 원칙**
> - **외부 동작은 절대 변경하지 않는다.** 입력/출력/예외 동작이 동일해야 한다.
> - **테스트가 안전망이다.** 모든 변경 후 전체 테스트가 그대로 통과해야 한다.
> - **작은 단위로, 자주.** 한 번에 큰 변경을 하지 말고 작은 변경 → 테스트 실행을 반복한다.
> - **테스트 코드도 리팩터링 대상이다.** 단, 프로덕션 코드와 테스트 코드를 동시에 바꾸지 않는다.
> - **새 기능 추가 금지.** 새 기능은 다음 Red 사이클에서.

---

## 사전 확인

스킬 실행 전, 다음을 확인한다.

1. **모든 테스트가 통과(Green) 상태인가?** 실패하는 테스트가 있으면 먼저 해결한다.
2. **테스트 커버리지가 충분한가?** 리팩터링 대상 코드의 주요 경로가 테스트로 검증되고 있어야 한다.
3. **리팩터링 범위가 명확한가?** 한 번에 전체를 갈아엎지 않고, 특정 메서드·클래스부터 시작한다.

테스트가 Green이 아니면 사용자에게 안내한다.

> "Refactor 단계는 모든 테스트가 통과한 상태에서 진행합니다.  
> 현재 실패하는 테스트가 있으니, 먼저 `tdd-green` 스킬로 통과시킨 뒤 진행하세요."

---

## 리팩터링 흐름

### 기본 사이클

```
1. 개선할 대상 선정 (Code Smell 식별)
2. 작은 변경 적용
3. 전체 테스트 실행
4. 통과하면 다음 변경 / 실패하면 즉시 되돌리기
5. 더 이상 개선할 게 없을 때까지 반복
```

> **한 번에 하나의 변경만.** 두 가지 변경을 동시에 했다가 테스트가 깨지면 어느 쪽이 원인인지 찾기 어렵다.

---

## Code Smell 식별 체크리스트

리팩터링 대상을 찾을 때 다음 항목을 점검한다.

### 메서드 레벨

| Smell | 설명 | 대응 기법 |
|-------|------|-----------|
| **Long Method** | 메서드가 한 화면을 넘어감 | Extract Method |
| **여러 책임 혼재** | 검증·계산·저장이 한 메서드에 | Extract Method, 책임 분리 |
| **Magic Number/String** | `"재고 부족"`, `100` 같은 리터럴 | 상수 추출 |
| **중첩된 if/else** | depth 3 이상의 분기 | Early Return, Guard Clause |
| **Boolean Flag 인자** | `doSomething(true)` 같은 호출 | 메서드 분리 |

### 클래스 레벨

| Smell | 설명 | 대응 기법 |
|-------|------|-----------|
| **Feature Envy** | 다른 클래스의 데이터만 만지는 메서드 | Move Method |
| **Primitive Obsession** | 모든 걸 String/int로 표현 | Value Object 도입 |
| **Data Clump** | 항상 함께 다니는 파라미터 그룹 | 파라미터 객체로 묶기 |
| **God Class** | 너무 많은 책임을 가진 클래스 | 클래스 분리 |

### 테스트 코드

| Smell | 설명 | 대응 기법 |
|-------|------|-----------|
| **중복된 given 블록** | 여러 테스트에 같은 setup | `@BeforeEach`, 픽스처 메서드 추출 |
| **불명확한 테스트명** | `test1`, `testCreate` | 의도가 드러나는 이름으로 변경 |
| **Magic Value in Test** | 의미 없는 `1L`, `"abc"` | 상수 또는 의미 있는 변수명 |
| **흩어진 같은 도메인 픽스처** | `OrderRequest` 생성이 여러 테스트에 중복 | Object Mother (`OrderFixture`) 추출 |

---

## 주요 리팩터링 기법

### 1. Extract Method (메서드 추출)

여러 책임이 섞인 메서드를 의미 단위로 분리한다.

**Before**
```java
public OrderResponse createOrder(OrderRequest request) {
    if (request.getUserId() == null) {
        throw new IllegalArgumentException("userId는 필수입니다");
    }

    Stock stock = stockRepository.findByProductId(request.getProductId())
        .orElseThrow(() -> new IllegalArgumentException("상품 없음"));

    if (stock.getQuantity() < request.getQuantity()) {
        throw new InsufficientStockException("재고가 부족합니다");
    }

    Order order = new Order(request.getUserId(), request.getProductId(), request.getQuantity());
    orderRepository.save(order);

    return new OrderResponse(request.getUserId(), OrderStatus.PENDING);
}
```

**After**
```java
public OrderResponse createOrder(OrderRequest request) {
    validate(request);
    checkStock(request);
    Order order = saveOrder(request);
    return OrderResponse.from(order);
}

private void validate(OrderRequest request) {
    if (request.getUserId() == null) {
        throw new IllegalArgumentException("userId는 필수입니다");
    }
}

private void checkStock(OrderRequest request) {
    Stock stock = stockRepository.findByProductId(request.getProductId())
        .orElseThrow(() -> new IllegalArgumentException("상품 없음"));

    if (stock.getQuantity() < request.getQuantity()) {
        throw new InsufficientStockException("재고가 부족합니다");
    }
}

private Order saveOrder(OrderRequest request) {
    Order order = new Order(request.getUserId(), request.getProductId(), request.getQuantity());
    return orderRepository.save(order);
}
```

상위 메서드가 **"무엇을 하는지"** 의 흐름으로 읽힌다.

### 2. 상수 추출 (Magic Value 제거)

예외 메시지·매직 넘버를 상수로 분리한다.

**Before**
```java
if (stock.getQuantity() < request.getQuantity()) {
    throw new InsufficientStockException("재고가 부족합니다");
}
```

**After**
```java
private static final String INSUFFICIENT_STOCK_MESSAGE = "재고가 부족합니다";

if (stock.getQuantity() < request.getQuantity()) {
    throw new InsufficientStockException(INSUFFICIENT_STOCK_MESSAGE);
}
```

> 다만 한 곳에서만 쓰이는 매직 값까지 무조건 상수로 빼지는 않는다.  
> "두 곳 이상에서 같은 값을 쓴다" 또는 "값의 의미가 이름 없이 알기 어렵다"가 추출 기준이다.

### 3. 책임 이동 (Move Method)

검증 로직이 Service에 있지만 사실 Domain의 책임인 경우, 도메인 객체로 옮긴다.

**Before**
```java
// OrderService
private void checkStock(OrderRequest request) {
    Stock stock = stockRepository.findByProductId(request.getProductId())
        .orElseThrow(...);

    if (stock.getQuantity() < request.getQuantity()) {
        throw new InsufficientStockException("재고가 부족합니다");
    }
}
```

**After**
```java
// Stock (도메인 객체)
public void ensureAvailable(int requestedQuantity) {
    if (this.quantity < requestedQuantity) {
        throw new InsufficientStockException("재고가 부족합니다");
    }
}

// OrderService
private void checkStock(OrderRequest request) {
    Stock stock = stockRepository.findByProductId(request.getProductId())
        .orElseThrow(...);
    stock.ensureAvailable(request.getQuantity());
}
```

> 도메인 객체가 "데이터 묶음(Anemic Domain Model)"에서 "행동을 가진 객체"가 된다.

### 4. Guard Clause / Early Return

중첩 if를 평탄화한다.

**Before**
```java
public OrderResponse createOrder(OrderRequest request) {
    if (request.getUserId() != null) {
        if (request.getProductId() != null) {
            if (request.getQuantity() > 0) {
                // 본 로직
            } else {
                throw new IllegalArgumentException("수량 오류");
            }
        } else {
            throw new IllegalArgumentException("상품 ID 누락");
        }
    } else {
        throw new IllegalArgumentException("사용자 ID 누락");
    }
}
```

**After**
```java
public OrderResponse createOrder(OrderRequest request) {
    if (request.getUserId() == null) {
        throw new IllegalArgumentException("사용자 ID 누락");
    }
    if (request.getProductId() == null) {
        throw new IllegalArgumentException("상품 ID 누락");
    }
    if (request.getQuantity() <= 0) {
        throw new IllegalArgumentException("수량 오류");
    }

    // 본 로직
}
```

### 5. DTO 변환 책임 분리

DTO 변환 로직이 Service에 흩어져 있다면 정적 팩토리 메서드로 옮긴다.

**Before**
```java
// Service 안에서
return new OrderResponse(order.getUserId(), order.getStatus());
```

**After**
```java
// OrderResponse
public static OrderResponse from(Order order) {
    return new OrderResponse(order.getUserId(), order.getStatus());
}

// Service
return OrderResponse.from(order);
```

### 6. 테스트 코드 리팩터링

#### 중복된 given 추출

**Before**
```java
@Test
void test1() {
    OrderRequest request = new OrderRequest(1L, 100L, 2);
    given(stockRepository.findByProductId(100L))
        .willReturn(Optional.of(new Stock(100L, 10)));
    // ...
}

@Test
void test2() {
    OrderRequest request = new OrderRequest(1L, 100L, 2);
    given(stockRepository.findByProductId(100L))
        .willReturn(Optional.of(new Stock(100L, 10)));
    // ...
}
```

**After**
```java
@BeforeEach
void setUp() {
    given(stockRepository.findByProductId(100L))
        .willReturn(Optional.of(new Stock(100L, 10)));
}

private OrderRequest validOrderRequest() {
    return new OrderRequest(1L, 100L, 2);
}
```

> Strict Stubbing 주의: `@BeforeEach`에서 stubbing한 Mock이 일부 테스트에서 호출되지 않으면 `UnnecessaryStubbingException`이 발생한다.  
> 이 경우 해당 stubbing을 각 테스트 내부로 옮기거나, `@Nested`로 분리해 setUp을 좁힌다.

#### Object Mother 패턴

픽스처가 여러 테스트 파일에 흩어져 있으면 별도 클래스로 추출한다.

```java
// src/test/java/com/example/order/fixture/OrderFixture.java
public class OrderFixture {

    public static OrderRequest validRequest() {
        return new OrderRequest(1L, 100L, 2);
    }

    public static OrderRequest requestWithQuantity(int quantity) {
        return new OrderRequest(1L, 100L, quantity);
    }

    public static Stock stockWith(int quantity) {
        return new Stock(100L, quantity);
    }
}
```

---

## 흔한 함정과 대응

### 함정 1: 리팩터링 중 새 기능 추가

"이왕 손대는 김에" 새 검증·새 메서드를 추가하고 싶어진다. 절대 금지.  
새 기능은 새 Red 테스트와 함께 다음 사이클에서.

### 함정 2: 프로덕션 코드와 테스트 코드 동시 변경

둘을 동시에 바꾸면 어느 쪽이 잘못됐는지 추적이 안 된다.  
**프로덕션 코드 리팩터링 → 테스트 통과 확인 → 테스트 코드 리팩터링 → 테스트 통과 확인** 순서를 지킨다.

### 함정 3: 한 번에 큰 변경

전체를 다 갈아엎고 테스트를 돌리는 방식은 위험하다.  
**메서드 하나 추출 → 테스트 / 상수 하나 추출 → 테스트** 식의 작은 사이클을 유지한다.

### 함정 4: 과도한 추상화

"미래에 확장될 것 같으니" 인터페이스·추상 클래스를 미리 만드는 건 YAGNI 위반이다.  
지금 코드에 명백한 중복·문제가 있을 때만 추상화한다.

### 함정 5: 테스트가 깨졌는데 "사소한 거니까" 무시

"이 변경은 동작에 영향 없을 거야"라며 깨진 테스트를 그대로 두면 안 된다.  
테스트가 깨졌다는 건 **외부 동작이 바뀌었다는 신호**다. 즉시 변경을 되돌리고 다시 검토한다.

---

## 리팩터링 우선순위

모든 코드를 완벽하게 다듬을 수는 없다. 다음 순서로 우선순위를 정한다.

1. **버그를 부르는 구조** — 중첩 분기, 예외 누락 가능성 → 최우선
2. **자주 변경되는 코드** — 같은 부분을 또 손볼 가능성 높음 → 다듬어두면 이득
3. **읽기 어려운 코드** — 다음 사람이 이해 못 하는 코드 → 가독성 개선
4. **중복 코드** — 3번 이상 반복되면 추출 (Rule of Three)
5. **사소한 스타일** — 변수명, 띄어쓰기 → 시간 남을 때

> "지금 손대지 않아도 되는 코드"는 그대로 둔다. Refactor 단계의 목적은 **이번에 추가된 변경 주변**을 정리하는 것이지, 코드베이스 전체 청소가 아니다.

---

## 출력 형식

리팩터링 완료 후 아래 형식으로 요약 출력한다.

```
🔧 Refactor 단계 완료

대상: OrderService#createOrder

적용한 리팩터링:
  ✅ Extract Method — validate, checkStock, saveOrder로 책임 분리
  ✅ 상수 추출 — 예외 메시지를 static final로 분리
  ✅ Move Method — 재고 검증을 Stock 도메인으로 이동 (ensureAvailable)
  ✅ DTO 변환 — OrderResponse.from(order) 정적 팩토리 메서드 도입
  ✅ 테스트 픽스처 — OrderFixture 클래스로 공통 데이터 분리

테스트 상태: 전체 Green 유지 (12/12 통과)
다음 단계: 새 기능이 필요하면 tdd-red로 다음 사이클 시작
```

---

## 다음 단계 안내

Refactor 완료 후 사용자에게 안내한다.

> "리팩터링이 완료됐고 모든 테스트가 그대로 통과합니다.  
> 한 사이클(Red → Green → Refactor)이 끝났습니다.  
> 새로운 기능이 필요하면 **tdd-red** 스킬로 다음 사이클을 시작하세요."