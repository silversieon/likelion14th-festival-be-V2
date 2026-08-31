/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency;

import static com.skulikelion.festival.domain.order.service.OrderFixture.dummyResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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

import com.skulikelion.festival.domain.order.dto.response.OrderResponse;
import com.skulikelion.festival.domain.order.repository.OrderRepository;
import com.skulikelion.festival.domain.support.IntegrationTestSupport;
import com.skulikelion.festival.global.util.idempotency.strategy.db.DbIdempotencyStrategy;
import com.skulikelion.festival.global.util.idempotency.strategy.db.repository.IdempotencyRepository;

public class DbIdempotentServiceIntegrationTest extends IntegrationTestSupport {

  @Autowired DbIdempotencyStrategy strategy;

  @Autowired OrderRepository orderRepository;

  @Autowired IdempotencyRepository idempotencyRepository;

  @AfterEach
  void tearDown() {
    orderRepository.deleteAll();
    idempotencyRepository.deleteAll();
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
    strategy.executeIdempotent(key, processor, OrderResponse.class);

    // then
    assertThat(callCount.get()).isEqualTo(1);
  }

  @Test
  @DisplayName("같은 멱등키로 두 번 요청하면 주문은 한 건만 생성된다")
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
    OrderResponse first = strategy.executeIdempotent(key, processor, OrderResponse.class);
    OrderResponse second = strategy.executeIdempotent(key, processor, OrderResponse.class);

    // then
    assertThat(callCount.get()).isEqualTo(1);
    assertThat(second).usingRecursiveComparison().isEqualTo(first);
  }

  @Test
  @DisplayName("같은 키로 동시에 20개 요청해도 processor는 한 번만 실행된다")
  void concurrent_sameKey_processorRunsOnce() throws InterruptedException {
    // given
    int threadCount = 20;
    String key = UUID.randomUUID().toString();
    AtomicInteger callCount = new AtomicInteger(0); // processor 실행 횟수
    AtomicInteger successCount = new AtomicInteger(0); // 정상 응답 받은 횟수
    AtomicInteger blockedCount = new AtomicInteger(0); // 중복으로 막힌 횟수

    Supplier<OrderResponse> processor =
        () -> {
          callCount.incrementAndGet();
          return dummyResponse();
        };

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1); // 출발 신호
    CountDownLatch doneLatch = new CountDownLatch(threadCount); // 완료 카운터

    // when — 20개 스레드를 출발선에 세워두고 일제히 발사
    for (int i = 0; i < threadCount; i++) {
      executor.submit(
          () -> {
            try {
              startLatch.await(); // 모든 스레드가 여기서 대기
              strategy.executeIdempotent(key, processor, OrderResponse.class);
              successCount.incrementAndGet();
            } catch (Exception e) {
              blockedCount.incrementAndGet(); // ALREADY_PROCESSING 등 중복 예외
            } finally {
              doneLatch.countDown();
            }
          });
    }

    startLatch.countDown(); // 20개 일제히 출발
    doneLatch.await(); // 20개 다 끝날 때까지 대기
    executor.shutdown();

    // then
    assertThat(callCount.get()).isEqualTo(1);
    System.out.println(
        "실행: " + callCount.get() + ", 성공: " + successCount.get() + ", 차단: " + blockedCount.get());
  }
}
