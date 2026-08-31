/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency.strategy.writethrough;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.global.util.idempotency.strategy.IdempotencyStrategy;
import com.skulikelion.festival.global.util.idempotency.strategy.IdempotencyType;
import com.skulikelion.festival.global.util.idempotency.strategy.db.DbIdempotencyExecutor;
import com.skulikelion.festival.global.util.idempotency.strategy.db.DbIdempotencyKeyManager;
import com.skulikelion.festival.global.util.idempotency.strategy.redis.RedisIdempotencyKeyManager;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WriteThroughIdempotencyStrategy implements IdempotencyStrategy {

  private final DbIdempotencyKeyManager dbIdempotencyKeyManager;
  private final RedisIdempotencyKeyManager redisIdempotencyKeyManager;
  private final DbIdempotencyExecutor dbIdempotencyExecutor;

  @Override
  public <T> T executeIdempotent(
      String idempotencyKey, Supplier<T> processor, Class<T> responseType) {
    if (!redisIdempotencyKeyManager.tryAcquire(idempotencyKey)) {
      return redisIdempotencyKeyManager.getCachedResponse(idempotencyKey, responseType);
    }

    if (!dbIdempotencyKeyManager.tryAcquire(idempotencyKey)) {
      T response = dbIdempotencyKeyManager.handleExisting(idempotencyKey, responseType);
      redisIdempotencyKeyManager.saveResponseIfAbsent(idempotencyKey, response);
      return response;
    }

    try {
      T response = dbIdempotencyExecutor.runAndMark(idempotencyKey, processor);
      redisIdempotencyKeyManager.saveResponse(idempotencyKey, response);
      return response;
    } catch (Exception e) {
      redisIdempotencyKeyManager.deleteKey(idempotencyKey);
      throw e;
    }
  }

  @Override
  public IdempotencyType getType() {
    return IdempotencyType.WRITETHROUGH;
  }
}
