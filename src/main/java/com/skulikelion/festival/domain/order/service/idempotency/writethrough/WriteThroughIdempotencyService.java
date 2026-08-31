/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.idempotency.writethrough;

import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.skulikelion.festival.domain.order.service.idempotency.IdempotencyService;
import com.skulikelion.festival.domain.order.service.idempotency.db.DbIdempotencyKeyManager;
import com.skulikelion.festival.domain.order.service.idempotency.redis.RedisIdempotencyKeyManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service("writeThroughIdempotencyService")
public class WriteThroughIdempotencyService implements IdempotencyService {

  private final WriteThroughExecutor writeThroughExecutor;
  private final RedisIdempotencyKeyManager redisIdempotencyKeyManager;
  private final DbIdempotencyKeyManager dbIdempotencyKeyManager;

  @Override
  public <T> T executeIdempotent(
      String idempotencyKey, Supplier<T> processor, Class<T> responseType) {

    if (!redisIdempotencyKeyManager.tryAcquire(idempotencyKey)) {
      return redisIdempotencyKeyManager.getCachedResponse(idempotencyKey, responseType);
    }

    UUID key = UUID.fromString(idempotencyKey);
    if (!dbIdempotencyKeyManager.tryAcquire(key)) {
      T response = dbIdempotencyKeyManager.handleExisting(key, responseType);
      redisIdempotencyKeyManager.saveResponseIfAbsent(idempotencyKey, response);
      return response;
    }

    try {
      T response = writeThroughExecutor.writeThrough(idempotencyKey, processor);
      redisIdempotencyKeyManager.saveResponse(idempotencyKey, response);
      return response;
    } catch (Exception e) {
      redisIdempotencyKeyManager.deleteKey(idempotencyKey);
      throw e;
    }
  }
}
