/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse;

import java.io.IOException;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.order.sse.store.OrderSseEmitterFinder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {

  private final OrderSseEmitterFinder finder;

  @Scheduled(fixedRate = 20_000)
  public void heartbeat() {
    for (SseEmitter emitter : finder.findAll()) {
      try {
        emitter.send(SseEmitter.event().comment("ping"));
      } catch (IOException | IllegalStateException e) {
        emitter.completeWithError(e);
      }
    }
  }
}
