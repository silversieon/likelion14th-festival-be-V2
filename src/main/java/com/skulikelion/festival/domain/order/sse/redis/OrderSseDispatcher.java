/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.order.sse.dto.SseEventMessage;
import com.skulikelion.festival.domain.order.sse.store.LocalOrderSseEmitterStore;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderSseDispatcher {

  private final LocalOrderSseEmitterStore store;

  public void dispatch(OrderSseChannelInfo info, SseEventMessage eventMessage) {
    List<SseEmitter> targets =
        store.findByBoothIdAndSubscribeType(info.boothId(), info.subscribeType());
    for (SseEmitter emitter : targets) {
      try {
        emitter.send(
            SseEmitter.event().name(eventMessage.eventName()).data(eventMessage.payload()));
      } catch (IOException e) {
        store.remove(info.boothId(), info.subscribeType(), emitter);
      }
    }
  }
}
