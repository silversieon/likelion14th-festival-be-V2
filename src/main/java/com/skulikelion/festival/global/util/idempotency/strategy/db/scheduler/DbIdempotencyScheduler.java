/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency.strategy.db.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.skulikelion.festival.global.util.idempotency.strategy.db.DbIdempotencyKeyManager;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DbIdempotencyScheduler {

  private final DbIdempotencyKeyManager idempotencyKeyManager;

  @Scheduled(cron = "0 0 1 * * *")
  public void cleanupIdempotencyKeys() {
    idempotencyKeyManager.deleteExpiredIdempotencyKeys(LocalDateTime.now().minusMinutes(10));
  }
}
