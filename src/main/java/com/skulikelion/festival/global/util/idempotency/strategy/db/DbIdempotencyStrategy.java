/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency.strategy.db;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.global.util.idempotency.strategy.IdempotencyStrategy;
import com.skulikelion.festival.global.util.idempotency.strategy.IdempotencyType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DbIdempotencyStrategy implements IdempotencyStrategy {

  private final DbIdempotencyKeyManager idempotencyKeyManager;
  private final DbIdempotencyExecutor idempotencyExecutor;

  @Override
  public <T> T executeIdempotent(
      String idempotencyKey, Supplier<T> processor, Class<T> responseType) {

    if (!idempotencyKeyManager.tryAcquire(idempotencyKey)) {
      return idempotencyKeyManager.handleExisting(idempotencyKey, responseType);
    }

    try {
      return idempotencyExecutor.runAndMark(idempotencyKey, processor);
    } catch (Exception e) {
      idempotencyKeyManager.deleteKey(idempotencyKey);
      throw e;
    }
  }

  @Override
  public IdempotencyType getType() {
    return IdempotencyType.DB;
  }
}
