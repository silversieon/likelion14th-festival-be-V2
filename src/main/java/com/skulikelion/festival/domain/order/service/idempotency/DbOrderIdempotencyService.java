/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.idempotency;

import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.skulikelion.festival.domain.order.entity.OrderIdempotency;
import com.skulikelion.festival.domain.order.entity.enums.IdempotencyStatus;
import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.global.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "order.idempotency.store", havingValue = "db")
public class DbOrderIdempotencyService implements OrderIdempotencyService {

  private final DbOrderIdempotencyKeyManager idempotencyKeyManager;
  private final ObjectMapper objectMapper;
  private final DbOrderIdempotencyExecutor idempotencyExecutor;

  @Override
  public <T> T executeIdempotent(
      String idempotencyKey, Supplier<T> processor, Class<T> responseType) {
    UUID key = UUID.fromString(idempotencyKey);

    if (!idempotencyKeyManager.isNewRequest(key)) {
      return handleExisting(key, responseType);
    }

    try {
      return idempotencyExecutor.runAndMark(key, processor);
    } catch (Exception e) {
      idempotencyKeyManager.deleteKey(key);
      throw e;
    }
  }

  private <T> T handleExisting(UUID key, Class<T> responseType) {
    OrderIdempotency found = idempotencyKeyManager.findOrThrow(key);

    if (found.getIdempotencyStatus() == IdempotencyStatus.PROCESSING) {
      throw new CustomException(OrderErrorCode.ORDER_ALREADY_PROCESSING);
    }
    return objectMapper.readValue(found.getResponseBody(), responseType);
  }
}
