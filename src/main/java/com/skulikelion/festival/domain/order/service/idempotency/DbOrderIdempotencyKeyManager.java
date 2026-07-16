/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.idempotency;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.order.entity.OrderIdempotency;
import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.domain.order.repository.OrderIdempotencyRepository;
import com.skulikelion.festival.global.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DbOrderIdempotencyKeyManager {

  private final OrderIdempotencyRepository repository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean isNewRequest(UUID idempotencyKey) {
    try {
      repository.saveAndFlush(OrderIdempotency.processing(idempotencyKey));
      return true;
    } catch (DataIntegrityViolationException e) {
      log.warn("[OrderIdempotency] 중복 요청 감지 - idempotencyKey: {}", idempotencyKey);
      return false;
    }
  }

  @Transactional
  public void saveDoneResponse(UUID idempotencyKey, String response) {
    OrderIdempotency entity =
        repository
            .findById(idempotencyKey)
            .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_IDEMPOTENCY_KEY_EXPIRED));
    entity.markDone(response);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteKey(UUID idempotencyKey) {
    repository.deleteById(idempotencyKey);
  }

  @Transactional(readOnly = true)
  public OrderIdempotency findOrThrow(UUID idempotencyKey) {
    return repository
        .findById(idempotencyKey)
        .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_IDEMPOTENCY_KEY_EXPIRED));
  }
}
