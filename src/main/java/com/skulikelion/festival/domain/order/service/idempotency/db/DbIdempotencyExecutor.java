/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.idempotency.db;

import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class DbIdempotencyExecutor {

  private final DbIdempotencyKeyManager idempotencyKeyManager;
  private final ObjectMapper objectMapper;

  @Transactional
  public <T> T runAndMark(UUID key, Supplier<T> processor) {
    T response = processor.get();
    idempotencyKeyManager.saveDoneResponse(key, objectMapper.writeValueAsString(response));
    return response;
  }
}
