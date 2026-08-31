/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency.strategy.fallback;

import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.skulikelion.festival.global.util.idempotency.IdempotencyInterceptor;
import com.skulikelion.festival.global.util.idempotency.strategy.db.DbIdempotencyInterceptor;
import com.skulikelion.festival.global.util.idempotency.strategy.writethrough.WriteThroughIdempotencyInterceptor;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FallbackIdempotencyInterceptor implements IdempotencyInterceptor {

  private final WriteThroughIdempotencyInterceptor writeThrough;
  private final DbIdempotencyInterceptor db;
  private final CircuitBreaker circuitBreaker;
  private final Retry retry;

  public FallbackIdempotencyInterceptor(
      WriteThroughIdempotencyInterceptor writeThrough,
      DbIdempotencyInterceptor db,
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
