/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis.pubsub;

import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import com.skulikelion.festival.domain.order.sse.dto.SseEventMessage;
import com.skulikelion.festival.domain.order.sse.redis.OrderSseChannelInfo;
import com.skulikelion.festival.domain.order.sse.redis.OrderSseChannelResolver;
import com.skulikelion.festival.domain.order.sse.redis.OrderSseDispatcher;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
public class OrderPubSubListener implements MessageListener {

  private final OrderSseChannelResolver channelResolver;
  private final ObjectMapper objectMapper;
  private final OrderSseDispatcher dispatcher;

  @Override
  public void onMessage(Message message, byte @Nullable [] pattern) {
    String channel = new String(message.getChannel());
    OrderSseChannelInfo info = channelResolver.parse(channel);
    SseEventMessage eventMessage = objectMapper.readValue(message.getBody(), SseEventMessage.class);

    dispatcher.dispatch(info, eventMessage);
  }
}
