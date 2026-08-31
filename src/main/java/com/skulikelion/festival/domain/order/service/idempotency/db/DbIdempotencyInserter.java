/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.idempotency.db;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.order.entity.OrderIdempotency;
import com.skulikelion.festival.domain.order.repository.OrderIdempotencyRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DbOrderIdempotencyInserter {

  private final OrderIdempotencyRepository repository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void insertProcessing(UUID idempotencyKey) {
    repository.saveAndFlush(OrderIdempotency.processing(idempotencyKey));
  }
}
