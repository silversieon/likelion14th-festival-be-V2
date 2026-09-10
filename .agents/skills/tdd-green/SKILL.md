---
name: tdd-green
description: |
  tdd-red 스킬로 작성된 실패하는 단위 테스트를 통과시키기 위해 최소한의 구현 코드를 작성하는 스킬.
  이미 존재하는 테스트 코드를 분석해, 테스트가 통과하도록 Service 메서드 본문과 필요한 도메인 객체를 작성한다.
  과도한 일반화·확장은 하지 않으며, 오직 "지금 작성된 테스트를 통과시키는 데 필요한 만큼만" 구현한다.
  "tdd green 해줘", "테스트 통과시켜줘", "구현 채워줘", "이 테스트 통과하는 코드 짜줘",
  "green 단계 해줘", "최소 구현 해줘", "테스트 돌아가게 해줘" 등의 표현이 나오면 반드시 이 스킬을 사용할 것.
  Java + Spring Boot + JUnit5 + Mockito + AssertJ 환경을 기본으로 한다.
---

# TDD Green Skill

tdd-red 스킬로 작성된 **실패하는 테스트를 통과시키는 최소 구현**을 작성한다.

> **Green 단계의 핵심 원칙**
> - 오직 **현재 작성된 테스트를 통과시키는 데 필요한 코드만** 작성한다.
> - "더 나은 설계"는 다음 단계(tdd-refactor)에서 한다. 지금은 통과가 최우선이다.
> - 테스트 코드는 **절대 수정하지 않는다**. Green 단계에서 테스트를 고치는 건 TDD 사이클을 깨는 행위다.
    >   - 예외: 테스트 자체에 명백한 오타·컴파일 에러가 있는 경우만 사용자에게 확인 후 수정
> - "지저분한 코드도 OK". Fake it 'til you make it 원칙 — 일단 통과시키고 리팩터링은 나중에.
> - 테스트가 모두 Green이 된 후에 멈춘다. 추가 기능은 다음 Red 사이클에서.

---

## 사전 확인

스킬 실행 전, 다음을 확인한다.

1. **tdd-red 단계가 선행되었는가?** 테스트 파일이 존재해야 한다.
2. **테스트가 실제로 실행 가능한가?** (컴파일은 되고 assertion에서 실패하는 상태)
3. **테스트가 의도한 대로 실패하는가?** (예상한 assertion에서 실패해야 함)

테스트 파일이 없다면 사용자에게 안내한다.

> "Green 단계는 Red 단계 이후에 진행됩니다.  
> 먼저 `tdd-red` 스킬로 실패하는 테스트를 작성하세요."

---

## 작업 흐름

### 1. 테스트 분석

대상 테스트 파일을 읽고 다음을 파악한다.

- **검증 대상 메서드** (`@InjectMocks` + 호출 메서드)
- **각 테스트가 기대하는 동작**
    - 반환값 (`assertThat(result)...`)
    - 예외 (`assertThatThrownBy(...)...`)
    - Side Effect (`then(repository).should()...`, `ArgumentCaptor`)
- **Mock에 설정된 stubbing** — 어떤 메서드가 어떤 값을 반환하는지
- **의존 객체** (`@Mock` 필드 목록)

> 테스트를 정독하지 않으면 "테스트는 통과하지만 의도와 다른 구현"이 나온다.
> 모든 assertion과 verify를 빠짐없이 확인한다.

### 2. 구현 순서

테스트 케이스를 **단순한 것부터 하나씩** 통과시킨다.
ZOMBIES 순서에 따라 작성된 Red 테스트를 같은 순서로 처리한다.

```
Zero → One → Many → Boundaries → Interface → Exceptions
```

#### 단계별 진행

1. 첫 번째 테스트를 통과시키는 최소 코드 작성
2. 전체 테스트 실행 — 1번이 통과하는지, 다른 테스트는 어떻게 실패하는지 확인
3. 다음 테스트를 통과시키는 코드 추가
4. 다시 전체 테스트 실행
5. 모든 테스트가 Green이 될 때까지 반복

> 한 번에 모든 테스트를 통과시키려는 코드를 작성하지 않는다.
> 한 테스트씩 천천히 추가하면 실수가 줄고, 어떤 변경이 어떤 테스트를 깨뜨리는지 추적이 쉽다.

---

## 구현 작성 기준

### "최소 구현"의 의미

| 상황 | 잘못된 접근 | 올바른 접근 |
|------|-------------|-------------|
| 정상 케이스 1개 테스트 | 일반화된 로직 작성 | 하드코딩으로 통과시키기도 OK |
| 예외 케이스 추가 | "더 깔끔하게" 리팩터링 | 분기 추가만 |
| 비슷한 테스트 여러 개 | 미리 추상화 | 중복 허용, refactor에서 정리 |

#### Fake it 'til you make it 예시

테스트가 하나뿐일 때:

```java
// 테스트: createOrder가 OrderResponse를 반환한다
public OrderResponse createOrder(OrderRequest request) {
    return new OrderResponse(1L, OrderStatus.PENDING);  // 하드코딩
}
```

이게 부끄러워 보이지만 Green 단계에서는 **정답**이다.
다음 테스트가 추가되면서 자연스럽게 일반화된다.

```java
// 테스트 2 추가: userId가 응답에 그대로 담긴다
public OrderResponse createOrder(OrderRequest request) {
    return new OrderResponse(request.getUserId(), OrderStatus.PENDING);  // 일반화
}
```

> 이 과정을 **Triangulation(삼각측량)** 이라 한다.
> 여러 테스트가 추가되면서 구현이 점점 일반화되는 패턴.

### 작성 범위

#### ✅ 작성 가능

- **Service 메서드의 비즈니스 로직** (테스트 통과에 필요한 최소한)
- **도메인 엔티티의 메서드** (테스트가 호출하는 것만)
- **DTO의 필드 매핑 로직** (필요한 경우)
- **예외 클래스의 추가 생성자** (필요한 경우)

#### ❌ 작성 금지

| 금지 항목 | 이유 |
|-----------|------|
| 테스트가 검증하지 않는 기능 | YAGNI — 다음 사이클에서 |
| 미리 만드는 helper 메서드 | 중복이 3번 이상 보일 때 refactor에서 추출 |
| 방어 코드 (null 체크 등) | 테스트가 요구하지 않으면 작성 X |
| 로깅, 모니터링 코드 | 테스트 외 기능 |
| Repository 실제 구현 | 단위 테스트는 Mock 사용 — Repository는 인터페이스만 |

---

## 흔한 함정과 대응

### 함정 1: 테스트가 깨졌을 때 테스트를 수정하고 싶은 충동

테스트가 깨지면 **구현을 의심하기 전에 테스트가 옳은지** 본다.
다만 99%는 구현이 잘못된 것이다. 테스트 수정은 마지막 수단이다.

### 함정 2: "이왕 만드는 김에" 다른 메서드까지 구현

YAGNI 원칙. 지금 테스트가 요구하지 않는 메서드는 작성하지 않는다.
다음 Red 사이클에서 새 테스트가 추가될 때 만들면 된다.

### 함정 3: Mock의 stubbing을 무시하고 다른 경로로 구현

테스트가 `given(stockRepository.findByProductId(100L)).willReturn(...)`를 설정했다면,
구현은 **반드시 `stockRepository.findByProductId`를 호출**해야 한다.

다른 방식으로 우회하면 테스트는 통과해도 의도가 어긋난다.
(예: Repository를 호출하지 않고 하드코딩으로 통과시키는 경우)

### 함정 4: 한 번에 너무 많이 작성

전체 테스트를 한꺼번에 통과시키려다 실패하면 어디서 잘못됐는지 추적이 어렵다.
**한 테스트 → 실행 → 다음 테스트** 사이클을 지킨다.

---

## 구현 예시

tdd-red 단계에서 작성된 `OrderServiceTest`를 통과시키는 구현 예시다.

### 1단계: 첫 번째 테스트 통과

테스트:
```java
@Test
@DisplayName("OrderResponse를 반환한다")
void returnsOrderResponse() {
    // given
    OrderRequest request = validOrderRequest();  // (1L, 100L, 2)
    given(stockRepository.findByProductId(100L))
        .willReturn(Optional.of(new Stock(100L, 10)));

    // when
    OrderResponse response = orderService.createOrder(request);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getUserId()).isEqualTo(1L);
}
```

최소 구현:
```java
public OrderResponse createOrder(OrderRequest request) {
    stockRepository.findByProductId(request.getProductId());  // Mock 호출 충족
    return new OrderResponse(request.getUserId(), OrderStatus.PENDING);
}
```

### 2단계: 저장 검증 테스트 통과

테스트:
```java
@Test
@DisplayName("주문을 Repository에 저장하며 요청 정보가 그대로 매핑된다")
void savesOrderWithCorrectFields() {
    // ...
    ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
    then(orderRepository).should().save(captor.capture());

    Order saved = captor.getValue();
    assertThat(saved.getUserId()).isEqualTo(1L);
    assertThat(saved.getProductId()).isEqualTo(100L);
    assertThat(saved.getQuantity()).isEqualTo(2);
}
```

구현 추가:
```java
public OrderResponse createOrder(OrderRequest request) {
    stockRepository.findByProductId(request.getProductId());

    Order order = new Order(request.getUserId(), request.getProductId(), request.getQuantity());
    orderRepository.save(order);

    return new OrderResponse(request.getUserId(), OrderStatus.PENDING);
}
```

### 3단계: 재고 부족 예외 통과

테스트:
```java
@Test
@DisplayName("InsufficientStockException을 던진다")
void throwsInsufficientStockException() {
    given(stockRepository.findByProductId(100L))
        .willReturn(Optional.of(new Stock(100L, 1)));  // 재고 1, 요청 수량 2

    assertThatThrownBy(() -> orderService.createOrder(request))
        .isInstanceOf(InsufficientStockException.class)
        .hasMessageContaining("재고");
}
```

구현 추가:
```java
public OrderResponse createOrder(OrderRequest request) {
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

### 4단계: null userId 예외 통과

테스트:
```java
@Test
@DisplayName("IllegalArgumentException을 던진다")
void throwsIllegalArgumentException() {
    OrderRequest request = new OrderRequest(null, 100L, 2);
    assertThatThrownBy(() -> orderService.createOrder(request))
        .isInstanceOf(IllegalArgumentException.class);
}
```

구현 추가:
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

> 위 구현은 **의도적으로 다듬어지지 않은 상태**다.
> 검증 로직과 비즈니스 로직이 한 메서드에 섞여 있고, 매직 스트링도 있다.
> 이런 부분은 **tdd-refactor 단계**에서 정리한다.

---

## 출력 형식

구현 작성 후 아래 형식으로 요약 출력한다.

```
✅ Green 단계 완료

대상: OrderService#createOrder
파일: src/main/java/com/example/order/OrderService.java

통과시킨 테스트:
  ✅ [정상] OrderResponse를 반환한다
  ✅ [정상] 주문을 Repository에 저장하며 요청 정보가 그대로 매핑된다
  ✅ [예외] 재고가 부족하면 InsufficientStockException을 던진다
  ✅ [예외] 재고 부족 시 주문을 저장하지 않는다
  ✅ [예외] userId가 null이면 IllegalArgumentException을 던진다

현재 상태: 전체 테스트 Green
다음 단계: tdd-refactor 스킬로 코드 정리
```

---

## 다음 단계 안내

Green 완료 후 반드시 사용자에게 안내한다.

> "모든 테스트가 통과했습니다.  
> 현재 코드는 통과 우선으로 작성되어 중복·매직 스트링·책임 분리 등 개선 여지가 있습니다.  
> 테스트를 안전망 삼아 코드를 정리하려면 **tdd-refactor** 스킬을 사용하세요."