/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency.strategy.redis;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.global.util.idempotency.IdempotencyInterceptor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisIdempotencyInterceptor implements IdempotencyInterceptor {

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
