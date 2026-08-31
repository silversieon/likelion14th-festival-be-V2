/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency.strategy.db;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.global.util.idempotency.strategy.db.converter.DbKeyConverter;
import com.skulikelion.festival.global.util.idempotency.strategy.db.entity.Idempotency;
import com.skulikelion.festival.global.util.idempotency.strategy.db.repository.IdempotencyRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DbIdempotencyInserter {

  private final IdempotencyRepository repository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void insertProcessing(String idempotencyKey) {
    UUID keyAsUuid = DbKeyConverter.convertKeyToDbKey(idempotencyKey);
    repository.saveAndFlush(Idempotency.processing(keyAsUuid));
  }
}
