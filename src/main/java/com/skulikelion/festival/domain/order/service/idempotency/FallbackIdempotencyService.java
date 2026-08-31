/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.idempotency;

import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.skulikelion.festival.domain.order.service.idempotency.db.DbIdempotencyService;
import com.skulikelion.festival.domain.order.service.idempotency.writethrough.WriteThroughIdempotencyService;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("fallbackIdempotencyService")
public class FallbackIdempotencyService implements IdempotencyService {

  private final DbIdempotencyService db;
  private final WriteThroughIdempotencyService writeThrough;
  private final CircuitBreaker circuitBreaker;
  private final Retry retry;

  public FallbackIdempotencyService(
      @Qualifier("writeThroughIdempotencyService") WriteThroughIdempotencyService writeThrough,
      @Qualifier("dbIdempotencyService") DbIdempotencyService db,
      CircuitBreakerRegistry cbRegistry,
      RetryRegistry retryRegistry) {
    this.db = db;
    this.writeThrough = writeThrough;
    this.circuitBreaker = cbRegistry.circuitBreaker("redisIdempotency");
    this.retry = retryRegistry.retry("redisIdempotency");
  }

  @Override
  public <T> T executeIdempotent(
      String idempotencyKey, Supplier<T> processor, Class<T> responseType) {
    Supplier<T> resilienceDecorated =
        CircuitBreaker.decorateSupplier(
            circuitBreaker,
            Retry.decorateSupplier(
                retry,
                () -> writeThrough.executeIdempotent(idempotencyKey, processor, responseType)));
    try {
      return resilienceDecorated.get();
    } catch (DataAccessException | CallNotPermittedException e) {
      log.warn(
          "Redis 처리 실패({}), DB로 fallback. key={}", e.getClass().getSimpleName(), idempotencyKey, e);
      return db.executeIdempotent(idempotencyKey, processor, responseType);
    }
  }
}
