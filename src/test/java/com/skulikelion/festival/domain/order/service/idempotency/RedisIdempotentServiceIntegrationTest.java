/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.idempotency;

import static com.skulikelion.festival.domain.order.service.OrderFixture.dummyResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import com.skulikelion.festival.domain.order.dto.response.OrderResponse;
import com.skulikelion.festival.domain.order.service.idempotency.redis.RedisIdempotencyService;
import com.skulikelion.festival.domain.support.IntegrationTestSupport;

@TestPropertySource(properties = "order.idempotency.store=redis")
public class RedisIdempotencyServiceIntegrationTest extends IntegrationTestSupport {

  @Autowired RedisIdempotencyService idempotencyService;

  @Autowired RedisTemplate<String, String> redisTemplate;

  @AfterEach
  void tearDown() {
    Objects.requireNonNull(redisTemplate.getConnectionFactory())
        .getConnection()
        .serverCommands()
        .flushDb();
  }

  @Test
  @DisplayName("첫 요청만 단독으로 주문하면 성공한다")
  void onlyOneOrder() {
    // given
    String key = UUID.randomUUID().toString();
    AtomicInteger callCount = new AtomicInteger(0);

    Supplier<OrderResponse> processor =
        () -> {
          callCount.incrementAndGet();
          return dummyResponse();
        };
    // when
    idempotencyService.executeIdempotent(key, processor, OrderResponse.class);

    // then
    assertThat(callCount.get()).isEqualTo(1);
  }

  @Test
  @DisplayName("같은 키로 두 번 요청하면 실제 처리는 한 번만 수행되고 동일한 응답을 반환한다")
  void sameKey_twice_onlyOneOrder() {
    // given
    String key = UUID.randomUUID().toString();
    AtomicInteger callCount = new AtomicInteger(0);

    Supplier<OrderResponse> processor =
        () -> {
          callCount.incrementAndGet();
          return dummyResponse();
        };

    // when
    OrderResponse first = idempotencyService.executeIdempotent(key, processor, OrderResponse.class);
    OrderResponse second =
        idempotencyService.executeIdempotent(key, processor, OrderResponse.class);

    // then
    assertThat(callCount.get()).isEqualTo(1);
    assertThat(first).usingRecursiveComparison().isEqualTo(second);
  }

  @Test
  @DisplayName("같은 키로 동시에 20개 요청해도 processor는 한 번만 실행된다")
  void concurrent_sameKey_processorRunsOnce() throws InterruptedException {
    // given
    int threadCount = 20;
    String key = UUID.randomUUID().toString();
    AtomicInteger callCount = new AtomicInteger(0);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger blockedCount = new AtomicInteger(0);

    Supplier<OrderResponse> supplier =
        () -> {
          callCount.incrementAndGet();
          return dummyResponse();
        };

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);

    // when
    for (int i = 0; i < threadCount; i++) {
      executor.submit(
          () -> {
            try {
              startLatch.await();
              idempotencyService.executeIdempotent(key, supplier, OrderResponse.class);
              successCount.incrementAndGet();
            } catch (Exception e) {
              blockedCount.incrementAndGet();
            } finally {
              doneLatch.countDown();
            }
          });
    }

    startLatch.countDown();
    doneLatch.await();
    executor.shutdown();

    // then
    assertThat(callCount.get()).isEqualTo(1);
    System.out.println(
        "실행: " + callCount.get() + ", 성공: " + successCount.get() + ", 차단: " + blockedCount.get());
  }
}
