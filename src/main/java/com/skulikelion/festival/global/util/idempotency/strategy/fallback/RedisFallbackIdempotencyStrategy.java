/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency.strategy.fallback;

import java.util.function.Supplier;

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Component;

import com.skulikelion.festival.global.util.idempotency.strategy.IdempotencyStrategy;
import com.skulikelion.festival.global.util.idempotency.strategy.IdempotencyType;
import com.skulikelion.festival.global.util.idempotency.strategy.db.DbIdempotencyStrategy;
import com.skulikelion.festival.global.util.idempotency.strategy.redis.RedisIdempotencyStrategy;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RedisFallbackIdempotencyStrategy implements IdempotencyStrategy {

  private final RedisIdempotencyStrategy redis;
  private final DbIdempotencyStrategy db;
  private final CircuitBreaker circuitBreaker;
  private final Retry retry;

  public RedisFallbackIdempotencyStrategy(
      RedisIdempotencyStrategy redis,
      DbIdempotencyStrategy db,
      CircuitBreakerRegistry cbRegistry,
      RetryRegistry retryRegistry) {
    this.redis = redis;
    this.db = db;
    this.circuitBreaker = cbRegistry.circuitBreaker("redisIdempotency");
    this.retry = retryRegistry.retry("redisIdempotency");
  }

  @Override
  public <T> T executeIdempotent(
      String idempotencyKey, Supplier<T> processor, Class<T> responseType) {
    Supplier<T> redisDecorated =
        CircuitBreaker.decorateSupplier(
            circuitBreaker,
            Retry.decorateSupplier(
                retry, () -> redis.executeIdempotent(idempotencyKey, processor, responseType)));
    try {
      return redisDecorated.get();
    } catch (RedisConnectionFailureException | CallNotPermittedException e) {
      log.warn(
          "Redis 처리 실패({}), DB로 fallback. key={}", e.getClass().getSimpleName(), idempotencyKey, e);
      return db.executeIdempotent(idempotencyKey, processor, responseType);
    }
  }

  @Override
  public IdempotencyType getType() {
    return IdempotencyType.REDISFALLBACK;
  }
}
