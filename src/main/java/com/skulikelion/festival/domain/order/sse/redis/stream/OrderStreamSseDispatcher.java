/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis.stream;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.booth.service.booth.BoothService;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;
import com.skulikelion.festival.domain.order.sse.dto.SseEventMessage;
import com.skulikelion.festival.domain.order.sse.redis.OrderSseChannelInfo;
import com.skulikelion.festival.domain.order.sse.redis.OrderSseChannelResolver;
import com.skulikelion.festival.domain.order.sse.redis.OrderSseDispatcher;
import com.skulikelion.festival.domain.order.sse.store.LocalOrderSseEmitterStore;
import com.skulikelion.festival.global.security.AuthPrincipal;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
public class OrderStreamSseDispatcher implements OrderSseDispatcher {

  private final LocalOrderSseEmitterStore store;
  private final RedisTemplate<String, String> redisTemplate;
  private final BoothService boothService;
  private final OrderSseChannelResolver channelResolver;
  private final ObjectMapper objectMapper;

  private static final String FIELD_MESSAGE = "message";

  public void dispatch(OrderSseChannelInfo info, SseEventMessage eventMessage, String recordId) {
    List<SseEmitter> targets =
        store.findByBoothIdAndSubscribeType(info.boothId(), info.subscribeType());
    for (SseEmitter emitter : targets) {
      try {
        send(emitter, eventMessage, recordId);
      } catch (IOException e) {
        emitter.completeWithError(e);
      }
    }
  }

  @Override
  public void replay(
      AuthPrincipal principal,
      OrderSseSubscribeType subscribeType,
      String lastEventId,
      SseEmitter emitter) {
    Long boothId = boothService.getRequiredBooth(principal).getId();
    String streamKey = channelResolver.toChannel(boothId, subscribeType);
    // lastEventId 이후 범위 조회
    List<MapRecord<String, Object, Object>> messages =
        redisTemplate
            .opsForStream()
            .range(streamKey, Range.rightUnbounded(Range.Bound.exclusive(lastEventId)));
    // emitter.send 수행 (보내는 메서드 따로 없는지 확인, 없으면 하나 만들기 )
    for (MapRecord<String, Object, Object> message : messages) {
      String recordId = message.getId().getValue();
      Map<Object, Object> value = message.getValue();
      SseEventMessage eventMessage =
          objectMapper.readValue(value.get(FIELD_MESSAGE).toString(), SseEventMessage.class);
      try {
        send(emitter, eventMessage, recordId);
      } catch (IOException e) {
        emitter.completeWithError(e);
      }
    }
  }

  private void send(SseEmitter emitter, SseEventMessage eventMessage, String recordId)
      throws IOException {
    emitter.send(
        SseEmitter.event()
            .id(recordId)
            .name(eventMessage.eventName())
            .data(eventMessage.payload()));
  }
}
