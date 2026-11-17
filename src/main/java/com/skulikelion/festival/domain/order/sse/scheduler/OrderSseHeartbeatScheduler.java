/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.scheduler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.order.sse.store.OrderSseEmitterFinder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderSseHeartbeatScheduler {

  private final OrderSseEmitterFinder emitterFinder;

  @Scheduled(fixedDelay = 30_000)
  public void heartbeat() {
    Map<String, List<SseEmitter>> all = emitterFinder.findAll();
    for (Map.Entry<String, List<SseEmitter>> entry : all.entrySet()) {
      entry
          .getValue()
          .forEach(
              emitter -> {
                try {
                  emitter.send(SseEmitter.event().name("heartbeat").data("heartbeat"));
                } catch (IOException e) {
                  log.debug(
                      "[좀비 커넥션 제거] 부스 식별자: {}, 주문 상태: {} 현재 시각: {}",
                      entry.getKey().split(":")[0],
                      entry.getKey().split(":")[1],
                      System.currentTimeMillis());
                }
              });
    }
  }
}
