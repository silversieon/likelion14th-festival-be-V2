/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.idempotency.db;

import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.skulikelion.festival.domain.order.service.idempotency.IdempotencyService;

import lombok.RequiredArgsConstructor;

@Service("dbIdempotencyService")
@RequiredArgsConstructor
public class DbIdempotencyService implements IdempotencyService {

  private final DbIdempotencyKeyManager idempotencyKeyManager;
  private final DbIdempotencyExecutor idempotencyExecutor;

  @Override
  public <T> T executeIdempotent(
      String idempotencyKey, Supplier<T> processor, Class<T> responseType) {
    UUID key = UUID.fromString(idempotencyKey);

    if (!idempotencyKeyManager.tryAcquire(key)) {
      return idempotencyKeyManager.handleExisting(key, responseType);
    }

    try {
      return idempotencyExecutor.runAndMark(key, processor);
    } catch (Exception e) {
      idempotencyKeyManager.deleteKey(key);
      throw e;
    }
  }
}
