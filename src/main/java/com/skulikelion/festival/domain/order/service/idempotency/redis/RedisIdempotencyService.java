/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.idempotency.redis;

import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.skulikelion.festival.domain.order.service.idempotency.IdempotencyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("redisIdempotencyService")
@RequiredArgsConstructor
public class RedisIdempotencyService implements IdempotencyService {

  private final RedisIdempotencyKeyManager idempotencyKeyManager;

  @Override
  public <T> T executeIdempotent(
      String idempotencyKey, Supplier<T> processor, Class<T> responseType) {

    if (idempotencyKeyManager.tryAcquire(idempotencyKey)) {
      try {
        T response = processor.get();
        idempotencyKeyManager.saveResponse(idempotencyKey, response);
        return response;
      } catch (Exception e) {
        idempotencyKeyManager.deleteKey(idempotencyKey);
        throw e;
      }
    }
    return idempotencyKeyManager.getCachedResponse(idempotencyKey, responseType);
  }
}
