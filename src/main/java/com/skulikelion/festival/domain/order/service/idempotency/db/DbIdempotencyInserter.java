/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.idempotency.db;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.order.entity.Idempotency;
import com.skulikelion.festival.domain.order.repository.IdempotencyRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DbIdempotencyInserter {

  private final IdempotencyRepository repository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void insertProcessing(UUID key) {
    repository.saveAndFlush(Idempotency.processing(key));
  }
}
