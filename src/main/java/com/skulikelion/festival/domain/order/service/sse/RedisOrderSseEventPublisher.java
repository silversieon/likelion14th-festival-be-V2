/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.sse;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisOrderSseEventPublisher implements OrderSseEventPublisher {

  private final RedisTemplate<String, String> redisTemplate;
  private final OrderSseChannelResolver channelResolver;

  @Override
  public void publish(
      Long boothId, SseSubscribeType subscribeType, String eventName, Object payload) {
    String channel = channelResolver.toChannel(boothId, subscribeType);
    SseEventMessage message = new SseEventMessage(eventName, payload);
    redisTemplate.convertAndSend(channel, message);
  }
}
