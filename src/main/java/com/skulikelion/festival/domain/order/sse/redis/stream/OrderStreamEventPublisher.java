/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis.stream;

import java.util.Map;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;

import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;
import com.skulikelion.festival.domain.order.sse.dto.SseEventMessage;
import com.skulikelion.festival.domain.order.sse.redis.OrderSseChannelResolver;
import com.skulikelion.festival.domain.order.sse.redis.OrderSseEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
public class OrderStreamEventPublisher implements OrderSseEventPublisher {

  private final RedisTemplate<String, String> redisTemplate;
  private final OrderSseChannelResolver channelResolver;
  private final ObjectMapper objectMapper;

  private static final String FIELD_MESSAGE = "message";

  @Override
  public void publish(
      Long boothId, OrderSseSubscribeType subscribeType, String eventName, Object payload) {
    String streamKey = channelResolver.toChannel(boothId, subscribeType);
    SseEventMessage message = new SseEventMessage(eventName, payload);

    MapRecord<String, String, String> record =
        StreamRecords.newRecord()
            .ofMap(Map.of(FIELD_MESSAGE, objectMapper.writeValueAsString(message)))
            .withStreamKey(streamKey);
    redisTemplate.opsForStream().add(record);
  }
}
