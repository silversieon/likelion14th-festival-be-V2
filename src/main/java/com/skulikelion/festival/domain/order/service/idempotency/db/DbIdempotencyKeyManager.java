/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.idempotency.db;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.order.entity.Idempotency;
import com.skulikelion.festival.domain.order.entity.enums.IdempotencyStatus;
import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.domain.order.repository.IdempotencyRepository;
import com.skulikelion.festival.global.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class DbIdempotencyKeyManager {

  private final IdempotencyRepository repository;
  private final DbIdempotencyInserter inserter;
  private final ObjectMapper objectMapper;

  public boolean tryAcquire(UUID key) {
    try {
      inserter.insertProcessing(key);
      return true;
    } catch (DataIntegrityViolationException e) {
      log.warn("[OrderIdempotency] 중복 요청 감지 - idempotencyKey: {}", key);
      return false;
    }
  }

  @Transactional
  public void saveDoneResponse(UUID key, String response) {
    Idempotency entity =
        repository
            .findById(key)
            .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_IDEMPOTENCY_KEY_EXPIRED));
    entity.markDone(response);
  }

  @Transactional
  public void deleteKey(UUID key) {
    repository.deleteById(key);
  }

  @Transactional
  public void deleteExpiredIdempotencyKeys(LocalDateTime threshold) {
    int rowCount = repository.deleteByCreatedAtBefore(threshold);
    log.info("[OrderIdempotency] 오래된 멱등성 키 제거 - count: {}", rowCount);
  }

  @Transactional(readOnly = true)
  public <T> T handleExisting(UUID key, Class<T> responseType) {
    Idempotency found =
        repository
            .findById(key)
            .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_IDEMPOTENCY_KEY_EXPIRED));

    if (found.getStatus() == IdempotencyStatus.PROCESSING) {
      throw new CustomException(OrderErrorCode.ORDER_ALREADY_PROCESSING);
    }
    return objectMapper.readValue(found.getResponseBody(), responseType);
  }
}
