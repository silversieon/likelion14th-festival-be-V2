/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis;

import java.io.IOException;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.order.sse.dto.SseEventMessage;
import com.skulikelion.festival.domain.order.sse.store.LocalOrderSseEmitterStore;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class OrderPubSubListener implements MessageListener {

  private final LocalOrderSseEmitterStore store;
  private final OrderSseChannelResolver channelResolver;
  private final ObjectMapper objectMapper;

  @Override
  public void onMessage(Message message, byte @Nullable [] pattern) {
    String channel = new String(message.getChannel());
    OrderSseChannelInfo info = channelResolver.parse(channel);

    SseEventMessage eventMessage = objectMapper.readValue(message.getBody(), SseEventMessage.class);

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
