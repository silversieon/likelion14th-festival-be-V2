/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis.pubsub;

import java.io.IOException;

import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.booth.service.booth.BoothService;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscriber;
import com.skulikelion.festival.domain.order.sse.redis.OrderSseChannelResolver;
import com.skulikelion.festival.domain.order.sse.store.OrderSseEmitterRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OrderPubSubSseSubscriber implements OrderSseSubscriber {

  private final RedisMessageListenerContainer listenerContainer;
  private final OrderSseChannelResolver channelResolver;
  private final OrderSseEmitterRegistry registry;
  private final OrderPubSubListener listener;
  private final BoothService boothService;

  @Override
  public SseEmitter subscribeOrderStatus(
      String departmentName, OrderSseSubscribeType subscribeType) {
    Long boothId = boothService.getRequiredBooth(departmentName).getId();
    SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);

    boolean isFirst = registry.registerAndCheckFirst(boothId, subscribeType, emitter);
    ChannelTopic topic = new ChannelTopic(channelResolver.toChannel(boothId, subscribeType));

    if (isFirst) {
      listenerContainer.addMessageListener(listener, topic);
    }
    registerUnsubscribeCallback(emitter, boothId, subscribeType);
    connectCheck(emitter, boothId, subscribeType);
    return emitter;
  }

  private void registerUnsubscribeCallback(
      SseEmitter emitter, Long boothId, OrderSseSubscribeType subscribeType) {

    emitter.onCompletion(
        () -> {
          log.debug("[OrderSseService] SSE 연결 종료 - 부스 식별자: {}, 구독 타입: {}", boothId, subscribeType);
          boolean isLast = registry.removeAndCheckLast(boothId, subscribeType, emitter);
          if (isLast) {
            listenerContainer.removeMessageListener(listener);
          }
        });
    emitter.onTimeout(
        () -> {
          log.debug("[OrderSseService] SSE 타임아웃 - 부스 식별자: {}, 구독 타입: {}", boothId, subscribeType);
          boolean isLast = registry.removeAndCheckLast(boothId, subscribeType, emitter);
          if (isLast) {
            listenerContainer.removeMessageListener(listener);
          }
          emitter.complete();
        });
    emitter.onError(
        e -> {
          log.warn("[OrderSseService] SSE 에러 - 부스 식별자: {}, 구독 타입: {}", boothId, subscribeType);
          boolean isLast = registry.removeAndCheckLast(boothId, subscribeType, emitter);
          if (isLast) {
            listenerContainer.removeMessageListener(listener);
          }
          emitter.complete();
        });
  }

  private void connectCheck(SseEmitter emitter, Long boothId, OrderSseSubscribeType subscribeType) {
    try {
      emitter.send(SseEmitter.event().name("connect").data("connected order subscribe"));
    } catch (IOException e) {
      registry.remove(boothId, subscribeType, emitter);
    }
  }
}
