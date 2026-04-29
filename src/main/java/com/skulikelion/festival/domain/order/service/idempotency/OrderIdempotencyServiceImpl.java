/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.idempotency;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.global.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderIdempotencyServiceImpl implements OrderIdempotencyService {

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;
  private static final String IDEMPOTENCY_PREFIX = "IDEMPOTENCY:";
  private static final String ORDER_PROCESSING = "processing";

  @Override
  public boolean isNewRequest(String idempotencyKey) {
    return Boolean.TRUE.equals(
        redisTemplate
            .opsForValue()
            .setIfAbsent(
                IDEMPOTENCY_PREFIX + idempotencyKey, ORDER_PROCESSING, Duration.ofMinutes(1)));
  }

  @Override
  public <T> T getCachedResponse(String idempotencyKey, Class<T> responseType) {
    String cached = redisTemplate.opsForValue().get(IDEMPOTENCY_PREFIX + idempotencyKey);
    if (ORDER_PROCESSING.equals(cached)) {
      throw new CustomException(OrderErrorCode.ORDER_ALREADY_PROCESSING);
    }
    if (cached == null) {
      throw new CustomException(OrderErrorCode.ORDER_IDEMPOTENCY_KEY_EXPIRED);
    }
    log.info("[OrderIdempotencyService] 캐싱된 응답을 조회했습니다. - idempotencyKey: {}", idempotencyKey);
    return objectMapper.readValue(cached, responseType);
  }

  @Override
  public void saveResponse(String idempotencyKey, Object response) {
    redisTemplate
        .opsForValue()
        .set(
            IDEMPOTENCY_PREFIX + idempotencyKey,
            objectMapper.writeValueAsString(response),
            Duration.ofMinutes(10));
  }

  @Override
  public void deleteKey(String idempotencyKey) {
    redisTemplate.delete(IDEMPOTENCY_PREFIX + idempotencyKey);
  }
}
