/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.scheduler;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.booth.service.booth.BoothService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoothScheduler {

  private final BoothService boothService;

  @Scheduled(cron = "0 0 1 * * *")
  public void dailyBoothStatusToClose() {
    log.info("[BoothScheduler] 새벽 1시 모든 부스 일괄 영업 종료");
    boothService.allBoothStatusToClose();
  }

  @Scheduled(cron = "0 * * * * *")
  public void updateBoothStatusEveryMinute() {
    LocalDate today = LocalDate.now();
    LocalTime now = LocalTime.now().withSecond(0).withNano(0);
    boothService.updateBoothStatusToOpen(today, now);
    boothService.updateBoothStatusToClose(today, now);
  }
}
