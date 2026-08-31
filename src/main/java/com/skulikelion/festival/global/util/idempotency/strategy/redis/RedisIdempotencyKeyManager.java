/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency.strategy.redis;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.util.idempotency.strategy.db.entity.IdempotencyStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisIdempotencyKeyManager {

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;
  private static final String IDEMPOTENCY_PREFIX = "IDEMPOTENCY:";

  public boolean tryAcquire(String idempotencyKey) {
    boolean isNew =
        Boolean.TRUE.equals(
            redisTemplate
                .opsForValue()
                .setIfAbsent(
                    IDEMPOTENCY_PREFIX + idempotencyKey,
                    IdempotencyStatus.PROCESSING.name(),
                    Duration.ofMinutes(1)));
    if (!isNew) {
      log.warn("[OrderIdempotency] 중복 요청 감지 - idempotencyKey: {}", idempotencyKey);
    }
    return isNew;
  }

  public <T> T getCachedResponse(String idempotencyKey, Class<T> responseType) {
    String cached = redisTemplate.opsForValue().get(IDEMPOTENCY_PREFIX + idempotencyKey);
    if (IdempotencyStatus.PROCESSING.name().equals(cached)) {
      throw new CustomException(OrderErrorCode.ORDER_ALREADY_PROCESSING);
    }
    if (cached == null) {
      throw new CustomException(OrderErrorCode.ORDER_IDEMPOTENCY_KEY_EXPIRED);
    }
    log.debug("[OrderIdempotencyService] 캐싱된 응답 반환 - idempotencyKey: {}", idempotencyKey);
    return objectMapper.readValue(cached, responseType);
  }

  public void saveResponse(String idempotencyKey, Object response) {
    redisTemplate
        .opsForValue()
        .set(
            IDEMPOTENCY_PREFIX + idempotencyKey,
            objectMapper.writeValueAsString(response),
            Duration.ofMinutes(10));
    log.debug("[OrderIdempotencyService] 응답 캐싱 완료 - idempotencyKey: {}", idempotencyKey);
  }

  public void saveResponseIfAbsent(String idempotencyKey, Object response) {
    redisTemplate
        .opsForValue()
        .setIfAbsent(
            IDEMPOTENCY_PREFIX + idempotencyKey,
            objectMapper.writeValueAsString(response),
            Duration.ofMinutes(10));
    log.debug("[OrderIdempotencyService] DB 저장 내용 캐싱 완료 - idempotencyKey: {}", idempotencyKey);
  }

  public void deleteKey(String idempotencyKey) {
    redisTemplate.delete(IDEMPOTENCY_PREFIX + idempotencyKey);
    log.debug(
        "[OrderIdempotencyService] idempotencyKey 삭제 완료 - idempotencyKey: {}", idempotencyKey);
  }
}
