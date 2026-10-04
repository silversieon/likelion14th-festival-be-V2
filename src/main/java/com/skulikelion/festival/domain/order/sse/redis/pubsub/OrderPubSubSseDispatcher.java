/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis.pubsub;

import java.io.IOException;
import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.order.sse.dto.SseEventMessage;
import com.skulikelion.festival.domain.order.sse.redis.OrderSseChannelInfo;
import com.skulikelion.festival.domain.order.sse.redis.OrderSseDispatcher;
import com.skulikelion.festival.domain.order.sse.store.LocalOrderSseEmitterStore;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderPubSubSseDispatcher implements OrderSseDispatcher {

  private final LocalOrderSseEmitterStore store;

  public void dispatch(OrderSseChannelInfo info, SseEventMessage eventMessage, String recordId) {
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
