/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.infra.redis;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RedisRecoveryWatcher {

  private final CircuitBreaker circuitBreaker;
  private final RedisConnectionFactory connectionFactory;

  public RedisRecoveryWatcher(
      CircuitBreakerRegistry circuitBreakerRegistry, RedisConnectionFactory connectionFactory) {
    this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("redisIdempotency");
    this.connectionFactory = connectionFactory;
  }

  @Scheduled(fixedDelay = 9000_000)
  public void checkRedisHealth() {
    if (circuitBreaker.getState() != CircuitBreaker.State.OPEN) {
      return;
    }
    try (RedisConnection connection = connectionFactory.getConnection()) {
      connection.ping();
      circuitBreaker.transitionToClosedState();
      log.info("Redis 회복 확인, 서킷 CLOSED로 전환");
    } catch (Exception e) {
      log.debug("Redis 아직 회복 안 됨: {}", e.getMessage());
    }
  }
}
