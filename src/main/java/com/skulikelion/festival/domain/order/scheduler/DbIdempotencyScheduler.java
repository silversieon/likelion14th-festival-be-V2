/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.order.service.idempotency.db.DbIdempotencyKeyManager;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DbOrderIdempotencyScheduler {

  private final DbIdempotencyKeyManager idempotencyKeyManager;

  @Scheduled(cron = "0 0 1 * * *")
  public void cleanupIdempotencyKeys() {
    idempotencyKeyManager.deleteExpiredIdempotencyKeys(LocalDateTime.now().minusMinutes(10));
  }
}
