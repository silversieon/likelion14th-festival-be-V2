/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.idempotency.writethrough;

import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.order.service.idempotency.db.DbIdempotencyKeyManager;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class WriteThroughExecutor {

  private final ObjectMapper objectMapper;
  private final DbIdempotencyKeyManager dbIdempotencyKeyManager;

  @Transactional
  public <T> T writeThrough(String idempotencyKey, Supplier<T> processor) {
    UUID key = UUID.fromString(idempotencyKey);
    T response = processor.get();
    dbIdempotencyKeyManager.saveDoneResponse(key, objectMapper.writeValueAsString(response));
    return response;
  }
}
