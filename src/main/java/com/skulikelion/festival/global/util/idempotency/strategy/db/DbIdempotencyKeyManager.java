/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency.strategy.db;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.util.idempotency.strategy.db.converter.DbKeyConverter;
import com.skulikelion.festival.global.util.idempotency.strategy.db.entity.Idempotency;
import com.skulikelion.festival.global.util.idempotency.strategy.db.entity.IdempotencyStatus;
import com.skulikelion.festival.global.util.idempotency.strategy.db.repository.IdempotencyRepository;

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

  public boolean tryAcquire(String idempotencyKey) {
    try {
      inserter.insertProcessing(idempotencyKey);
      return true;
    } catch (DataIntegrityViolationException e) {
      log.debug("[OrderIdempotency] 중복 요청 감지 - idempotencyKey: {}", idempotencyKey);
      return false;
    }
  }

  @Transactional
  public <T> void saveDoneResponse(String idempotencyKey, T response) {
    UUID keyAsUuid = DbKeyConverter.convertKeyToDbKey(idempotencyKey);
    Idempotency entity =
        repository
            .findById(keyAsUuid)
            .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_IDEMPOTENCY_KEY_EXPIRED));
    entity.markDone(objectMapper.writeValueAsString(response));
  }

  @Transactional
  public void deleteKey(String idempotencyKey) {
    UUID keyAsUuid = DbKeyConverter.convertKeyToDbKey(idempotencyKey);
    repository.deleteById(keyAsUuid);
  }

  @Transactional
  public void deleteExpiredIdempotencyKeys(LocalDateTime threshold) {
    int rowCount = repository.deleteByCreatedAtBefore(threshold);
    log.debug("[OrderIdempotency] 오래된 멱등성 키 제거 - count: {}", rowCount);
  }

  @Transactional(readOnly = true)
  public <T> T handleExisting(String idempotencyKey, Class<T> responseType) {
    UUID keyAsUuid = DbKeyConverter.convertKeyToDbKey(idempotencyKey);
    Idempotency found =
        repository
            .findById(keyAsUuid)
            .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_IDEMPOTENCY_KEY_EXPIRED));

    if (found.getStatus() == IdempotencyStatus.PROCESSING) {
      throw new CustomException(OrderErrorCode.ORDER_ALREADY_PROCESSING);
    }
    return objectMapper.readValue(found.getResponseBody(), responseType);
  }
}
