---
name: tdd-red
description: |
  백엔드 특정 도메인(패키지)에 대해 TDD Red 단계의 단위 테스트 코드를 작성하는 스킬.
  도메인, 기능 설명, DTO, Service 메서드 시그니처 등을 사용자가 제공하면
  실행 가능한 상태에서 assertion이 실패하는 단위 테스트 코드를 실무 수준으로 생성한다.
  비즈니스 로직 구현은 절대 작성하지 않으며, 테스트 실행을 위한 최소 골격(skeleton)까지만 작성한다.
  "TDD 테스트 짜줘", "red 테스트 작성해줘", "단위테스트 먼저 짜줘", "테스트 코드 먼저 작성해줘",
  "tdd red 해줘", "실패하는 테스트 만들어줘", "테스트 먼저 써줘" 등의 표현이 나오면 반드시 이 스킬을 사용할 것.
  Java + Spring Boot + JUnit5 + Mockito + AssertJ 환경을 기본으로 한다.
---

# TDD Red Skill

사용자가 제공한 도메인 정보를 바탕으로 **실행되며 assertion에서 실패하는 단위 테스트(Red)**를 작성한다.

> **Red 단계의 핵심 원칙**
> - 테스트는 **컴파일되고 실행되어야** 한다. 그리고 assertion에서 실패해야 한다.
    >   - 컴파일 에러는 Red가 아니다. 테스트 러너가 돌지 않으면 "내 테스트가 의도한 실패를 잡아내는지" 확인할 수 없다.
> - 이를 위해 **메서드 시그니처가 담긴 빈 골격(skeleton)까지는 작성**한다.
> - 비즈니스 로직은 단 한 줄도 작성하지 않는다.
> - 테스트 하나는 하나의 동작(Single Behavior)만 검증한다.
    >   - 단, "save 호출 + 반환값 검증"처럼 같은 동작의 일부를 함께 검증하는 것은 허용한다.
> - 테스트 코드 자체가 요구사항 명세서 역할을 한다.

---

## 사전 정보 수집

스킬 실행 전, 아래 정보가 대화에 있는지 확인한다.
없는 항목은 사용자에게 질문해서 채운 뒤 진행한다.

| 항목 | 예시 | 필수 여부 |
|------|------|-----------|
| 도메인(패키지명) | `com.example.order` | 필수 |
| 대상 Service 클래스명 | `OrderService` | 필수 |
| 구현할 메서드 시그니처 | `OrderResponse createOrder(OrderRequest request)` | 필수 |
| DTO 필드 정보 | `OrderRequest(userId, productId, quantity)` | 필수 |
| 비즈니스 규칙 / 예외 조건 | "재고 부족 시 InsufficientStockException", "userId null이면 예외" | 필수 |
| 의존 객체 (Repository 등) | `OrderRepository`, `StockRepository` | 필수 |
| 기술 스택 | JUnit5 + Mockito + AssertJ (기본값) | 선택 — 없으면 기본값 사용 |

> 위 정보가 불충분한 채로 테스트를 작성하면 요구사항과 다른 테스트가 만들어진다.
> 반드시 확인 후 진행한다.

---

## Red 단계에서 작성 가능한 것 / 금지된 것

### ✅ 작성 가능

테스트가 컴파일되고 실행되도록 하기 위한 최소한의 코드는 작성한다.

- **DTO 클래스** (Request, Response) — Java 16+ 환경이면 `record` 권장
- **커스텀 예외 클래스** — 메시지를 받는 생성자만 포함
- **Service 클래스의 메서드 시그니처와 빈 골격**
  ```java
  public class OrderService {
      private final OrderRepository orderRepository;
      private final StockRepository stockRepository;

      public OrderService(OrderRepository orderRepository, StockRepository stockRepository) {
          this.orderRepository = orderRepository;
          this.stockRepository = stockRepository;
      }

      public OrderResponse createOrder(OrderRequest request) {
          throw new UnsupportedOperationException("Not implemented yet");
      }
  }
  ```
- **Repository 인터페이스** (메서드 시그니처만)

### ❌ 작성 금지

| 금지 항목 | 이유 |
|-----------|------|
| Service 메서드의 비즈니스 로직 | Red 단계는 테스트만 작성 |
| Repository 구현체 | Mock으로 대체 |
| `@SpringBootTest` | 단위 테스트에 통합 테스트 컨텍스트 불필요 |
| 테스트 내 비즈니스 로직 | 테스트가 구현을 강제해선 안 됨 |
| 여러 동작을 한 테스트에 묶기 | Single Behavior 원칙 위반 (같은 동작의 일부는 허용) |

---

## 테스트 설계 원칙

### 테스트 케이스 도출 순서 (ZOMBIES)

Kent Beck의 ZOMBIES 원칙에 따라 **단순한 케이스부터 복잡한 케이스로** 작성한다.

1. **Zero** — 비어있거나 0인 입력 (빈 리스트, null, 0개 아이템)
2. **One** — 단 하나의 정상 케이스 (Happy Path의 최소 단위)
3. **Many** — 여러 개의 정상 케이스 (Happy Path 확장)
4. **Boundaries** — 경계값 (최대값, 최소값, off-by-one 지점)
5. **Interface** — 외부와의 상호작용 (Repository 저장, 이벤트 발행)
6. **Exceptions** — 예외 케이스 (비즈니스 규칙 위반, 잘못된 입력)
7. **Simple scenarios** — 실제 사용 시나리오

> 경계값(Boundaries)을 예외(Exceptions)보다 먼저 작성하는 이유:
> 경계값은 "정상이지만 까다로운 입력"이라 Happy Path의 연장선이다.
> 예외는 별도의 흐름이므로 정상 흐름이 완성된 뒤 다루는 것이 자연스럽다.

### Given-When-Then 구조 준수

모든 테스트는 명확한 Given-When-Then 구조로 작성한다.

```java
@Test
@DisplayName("재고가 부족하면 InsufficientStockException을 던진다")
void createOrder_outOfStock_throwsInsufficientStockException() {
    // given
    // 테스트에 필요한 입력값, Mock 설정

    // when
    // 테스트 대상 메서드 호출

    // then
    // 결과 검증 (assert), 상호작용 검증 (verify)
}
```

### 네이밍 규칙

#### 메서드명 (영문)

```
<메서드명>_<테스트_상황>_<기대_결과>
```

예시:
- `createOrder_validRequest_returnsOrderResponse`
- `createOrder_outOfStock_throwsInsufficientStockException`
- `createOrder_nullUserId_throwsIllegalArgumentException`

#### `@DisplayName` (한글, 단언문)

테스트 리포트에서 비개발자(QA, PM)도 읽을 수 있도록 **한글 단언문**으로 작성한다.

```java
@DisplayName("유효한 주문 요청 시 OrderResponse를 반환한다")
@DisplayName("재고가 부족하면 InsufficientStockException을 던진다")
@DisplayName("userId가 null이면 IllegalArgumentException을 던진다")
```

> "테스트한다", "검증한다" 같은 메타 표현은 쓰지 않는다.
> 동작 자체를 단언문으로 서술한다.

---

## 파일 구조

테스트 파일은 `src/test` 하위에 도메인 패키지와 동일한 경로로 생성한다.

```
src/
├── main/java/com/example/order/
│   ├── OrderService.java          ← 골격만 (UnsupportedOperationException)
│   ├── dto/
│   │   ├── OrderRequest.java
│   │   └── OrderResponse.java
│   ├── exception/
│   │   └── InsufficientStockException.java
│   └── repository/
│       ├── OrderRepository.java   ← 인터페이스만
│       └── StockRepository.java
└── test/java/com/example/order/
    └── OrderServiceTest.java      ← 이 스킬이 생성하는 메인 파일
```

---

## 코드 작성 기준

### 클래스 레벨 구성

테스트 케이스가 3개를 넘어가면 `@Nested`로 그룹핑한다.

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private OrderService orderService;

    @Nested
    @DisplayName("createOrder 메서드는")
    class CreateOrder {

        @Nested
        @DisplayName("유효한 요청이 들어오면")
        class WhenValidRequest {

            @Test
            @DisplayName("OrderResponse를 반환한다")
            void returnsOrderResponse() {
                // given ...
                // when ...
                // then ...
            }

            @Test
            @DisplayName("주문을 Repository에 저장한다")
            void savesOrder() {
                // ...
            }
        }

        @Nested
        @DisplayName("재고가 부족하면")
        class WhenOutOfStock {

            @Test
            @DisplayName("InsufficientStockException을 던진다")
            void throwsException() {
                // ...
            }

            @Test
            @DisplayName("주문을 저장하지 않는다")
            void doesNotSaveOrder() {
                // ...
            }
        }
    }
}
```

### 테스트 픽스처 관리

공통 데이터는 **정적 팩토리 메서드**로 관리한다. 필드로 선언하거나 `@BeforeEach`에서 매번 생성하는 방식보다 가독성과 재사용성이 높다.

```java
// 기본 정상 데이터
private OrderRequest validOrderRequest() {
    return new OrderRequest(1L, 100L, 2);
}

// 일부 필드만 바꿔야 할 때 (오버로딩)
private OrderRequest orderRequestWith(Long userId) {
    return new OrderRequest(userId, 100L, 2);
}

private OrderRequest orderRequestWithQuantity(int quantity) {
    return new OrderRequest(1L, 100L, quantity);
}
```

> 픽스처 메서드가 많아지면 별도의 `OrderFixture` 클래스로 분리한다.
> 이를 Object Mother 패턴이라 부른다.

### Mock 설정 — BDDMockito 사용

`when(...).thenReturn(...)` 대신 BDDMockito의 `given(...).willReturn(...)`을 사용한다.
Given-When-Then 구조와 문법이 일치해 가독성이 높아진다.

```java
// 반환값이 있는 경우
given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

// void 메서드인 경우
willDoNothing().given(orderRepository).delete(order);

// 예외를 던져야 하는 경우
given(stockRepository.findByProductId(productId))
    .willThrow(new InsufficientStockException("재고 부족"));
```

#### Strict Stubbing 주의

`@ExtendWith(MockitoExtension.class)`는 기본적으로 **strict 모드**다. Mock을 stubbing해놓고 실제로 호출하지 않으면 `UnnecessaryStubbingException`이 발생한다.

```java
// ❌ stubbing해놓고 호출 안 하면 예외 발생
given(stockRepository.findByProductId(any())).willReturn(stock);

// ✅ 해당 테스트에서 실제로 호출되는 stubbing만 작성
// 필요하다면 @BeforeEach가 아닌 각 테스트 내부에서 stubbing
```

> `lenient().given(...)`으로 strict 모드를 우회할 수 있지만, 남용하면 테스트의 의도가 흐려진다.
> 꼭 필요한 경우에만 쓴다.

### 검증 — AssertJ 사용

`assertEquals` 대신 AssertJ의 `assertThat`을 사용한다. 실패 메시지가 명확하고 체이닝으로 가독성이 높다.

```java
// 반환값 검증
assertThat(result.getOrderId()).isNotNull();
assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
assertThat(result.getItems()).hasSize(3);

// 예외 검증
assertThatThrownBy(() -> orderService.createOrder(invalidRequest))
    .isInstanceOf(InsufficientStockException.class)
    .hasMessageContaining("재고 부족");

// 상호작용 검증 (저장 호출 여부)
then(orderRepository).should(times(1)).save(any(Order.class));

// 호출되지 않아야 하는 경우
then(orderRepository).should(never()).save(any());
```

### ArgumentCaptor — 저장된 객체의 필드 검증

"Repository에 저장되는 객체의 특정 필드 값이 올바른가"를 검증할 때 사용한다.
Side Effect 검증의 핵심 도구다.

```java
@Test
@DisplayName("주문 생성 시 요청의 userId가 그대로 저장된다")
void createOrder_validRequest_savesOrderWithCorrectUserId() {
    // given
    OrderRequest request = validOrderRequest();
    given(stockRepository.findByProductId(100L))
        .willReturn(Optional.of(new Stock(100L, 10)));

    // when
    orderService.createOrder(request);

    // then
    ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
    then(orderRepository).should().save(captor.capture());

    Order saved = captor.getValue();
    assertThat(saved.getUserId()).isEqualTo(1L);
    assertThat(saved.getProductId()).isEqualTo(100L);
    assertThat(saved.getQuantity()).isEqualTo(2);
}
```

---

## 전체 예시

다음은 `OrderService.createOrder`에 대한 Red 단계 테스트의 완성 형태다.

```java
package com.example.order;

import com.example.order.dto.OrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.exception.InsufficientStockException;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private OrderService orderService;

    private OrderRequest validOrderRequest() {
        return new OrderRequest(1L, 100L, 2);
    }

    @Nested
    @DisplayName("createOrder 메서드는")
    class CreateOrder {

        @Nested
        @DisplayName("유효한 요청이 들어오면")
        class WhenValidRequest {

            @Test
            @DisplayName("OrderResponse를 반환한다")
            void returnsOrderResponse() {
                // given
                OrderRequest request = validOrderRequest();
                given(stockRepository.findByProductId(100L))
                    .willReturn(Optional.of(new Stock(100L, 10)));

                // when
                OrderResponse response = orderService.createOrder(request);

                // then
                assertThat(response).isNotNull();
                assertThat(response.getUserId()).isEqualTo(1L);
            }

            @Test
            @DisplayName("주문을 Repository에 저장하며 요청 정보가 그대로 매핑된다")
            void savesOrderWithCorrectFields() {
                // given
                OrderRequest request = validOrderRequest();
                given(stockRepository.findByProductId(100L))
                    .willReturn(Optional.of(new Stock(100L, 10)));

                // when
                orderService.createOrder(request);

                // then
                ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
                then(orderRepository).should().save(captor.capture());

                Order saved = captor.getValue();
                assertThat(saved.getUserId()).isEqualTo(1L);
                assertThat(saved.getProductId()).isEqualTo(100L);
                assertThat(saved.getQuantity()).isEqualTo(2);
            }
        }

        @Nested
        @DisplayName("재고가 부족하면")
        class WhenOutOfStock {

            @BeforeEach
            void setUp() {
                given(stockRepository.findByProductId(100L))
                    .willReturn(Optional.of(new Stock(100L, 1)));
            }

            @Test
            @DisplayName("InsufficientStockException을 던진다")
            void throwsInsufficientStockException() {
                assertThatThrownBy(() -> orderService.createOrder(validOrderRequest()))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("재고");
            }

            @Test
            @DisplayName("주문을 저장하지 않는다")
            void doesNotSaveOrder() {
                assertThatThrownBy(() -> orderService.createOrder(validOrderRequest()))
                    .isInstanceOf(InsufficientStockException.class);

                then(orderRepository).should(never()).save(any());
            }
        }

        @Nested
        @DisplayName("userId가 null이면")
        class WhenNullUserId {

            @Test
            @DisplayName("IllegalArgumentException을 던진다")
            void throwsIllegalArgumentException() {
                // given
                OrderRequest request = new OrderRequest(null, 100L, 2);

                // when & then
                assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(IllegalArgumentException.class);
            }
        }
    }
}
```

---

## 출력 형식

테스트 코드 작성 후 아래 형식으로 요약 출력한다.

```
📋 Red 테스트 작성 완료

대상: OrderService#createOrder
파일: src/test/java/com/example/order/OrderServiceTest.java
골격 파일: src/main/java/com/example/order/OrderService.java
       (UnsupportedOperationException으로 채워진 빈 메서드)

작성된 테스트 케이스:
  ✅ [정상] OrderResponse를 반환한다
  ✅ [정상] 주문을 Repository에 저장하며 요청 정보가 그대로 매핑된다
  ✅ [예외] 재고가 부족하면 InsufficientStockException을 던진다
  ✅ [예외] 재고 부족 시 주문을 저장하지 않는다
  ✅ [예외] userId가 null이면 IllegalArgumentException을 던진다

현재 상태: 테스트 실행 가능 / assertion에서 실패
다음 단계: tdd-green 스킬로 최소 구현 작성
```

---

## 다음 단계 안내

Red 테스트 작성 완료 후 반드시 사용자에게 안내한다.

> "테스트 코드 작성이 완료됐습니다.
> 골격 코드(`UnsupportedOperationException`)까지 작성되어 테스트는 실행되며,
> assertion 단계에서 실패하는 정상적인 Red 상태입니다.
> 최소 구현으로 테스트를 통과시키려면 **tdd-green** 스킬을 사용하세요."