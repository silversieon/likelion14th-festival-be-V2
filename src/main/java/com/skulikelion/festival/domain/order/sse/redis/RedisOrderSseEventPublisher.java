/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis;

import org.springframework.data.redis.core.RedisTemplate;

import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;
import com.skulikelion.festival.domain.order.sse.dto.SseEventMessage;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
public class RedisOrderSseEventPublisher implements OrderSseEventPublisher {

  private final RedisTemplate<String, String> redisTemplate;
  private final OrderSseChannelResolver channelResolver;
  private final ObjectMapper objectMapper;

  @Override
  public void publish(
      Long boothId, OrderSseSubscribeType subscribeType, String eventName, Object payload) {
    String channel = channelResolver.toChannel(boothId, subscribeType);
    SseEventMessage message = new SseEventMessage(eventName, payload);
    redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(message));
  }
}
