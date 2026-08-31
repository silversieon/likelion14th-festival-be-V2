/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency.strategy.db;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DbIdempotencyExecutor {

  private final DbIdempotencyKeyManager idempotencyKeyManager;

  @Transactional
  public <T> T runAndMark(String idempotencyKey, Supplier<T> processor) {
    T response = processor.get();
    idempotencyKeyManager.saveDoneResponse(idempotencyKey, response);
    return response;
  }
}
