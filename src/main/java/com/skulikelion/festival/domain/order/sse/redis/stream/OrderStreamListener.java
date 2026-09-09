/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis.stream;

import java.util.Map;
import java.util.Objects;

import jakarta.annotation.PostConstruct;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;

import com.skulikelion.festival.domain.order.sse.dto.SseEventMessage;
import com.skulikelion.festival.domain.order.sse.redis.OrderSseChannelInfo;
import com.skulikelion.festival.domain.order.sse.redis.OrderSseChannelResolver;
import com.skulikelion.festival.domain.order.sse.redis.OrderSseDispatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
public class OrderStreamListener
    implements StreamListener<String, MapRecord<String, String, String>> {

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;
  private final OrderSseChannelResolver channelResolver;
  private final String instanceId;
  private final OrderSseDispatcher dispatcher;
  private static final String FIELD_MESSAGE = "message";

  private String groupName;

  @PostConstruct
  void init() {
    this.groupName = "sse-consumer-group-" + instanceId;
  }

  @Override
  public void onMessage(MapRecord<String, String, String> message) {
    try {
      String stream = message.getStream();
      OrderSseChannelInfo info = channelResolver.parse(Objects.requireNonNull(stream));
      Map<String, String> value = message.getValue();
      SseEventMessage eventMessage =
          objectMapper.readValue(value.get(FIELD_MESSAGE), SseEventMessage.class);

      dispatcher.dispatch(info, eventMessage);

      // 처리 성공 시 ACK -> 유실 방지 핵심 포인트
      redisTemplate.opsForStream().acknowledge(groupName, message);
    } catch (Exception e) {
      log.error("메시지 처리 실패, ack 안 함 -> 재처리 대상", e);
    }
  }
}
