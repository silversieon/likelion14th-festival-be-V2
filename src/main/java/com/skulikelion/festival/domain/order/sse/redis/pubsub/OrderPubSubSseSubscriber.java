/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis;

import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.booth.service.booth.BoothService;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscriber;
import com.skulikelion.festival.domain.order.sse.local.LocalOrderSseSubscriber;
import com.skulikelion.festival.domain.order.sse.store.OrderSseEmitterFinder;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderPubSubSseSubscriber implements OrderSseSubscriber {

  private final LocalOrderSseSubscriber localSubscriber;
  private final RedisMessageListenerContainer listenerContainer;
  private final OrderSseChannelResolver channelResolver;
  private final OrderSseEmitterFinder emitterFinder;
  private final OrderPubSubListener listener;
  private final BoothService boothService;

  @Override
  public SseEmitter subscribeOrderStatus(
      String departmentName, OrderSseSubscribeType subscribeType) {
    SseEmitter emitter = localSubscriber.subscribeOrderStatus(departmentName, subscribeType);

    Long boothId = boothService.getRequiredBooth(departmentName).getId();
    ChannelTopic topic = new ChannelTopic(channelResolver.toChannel(boothId, subscribeType));

    subscribeIfFirst(boothId, subscribeType, topic);
    registerUnsubscribeCallback(emitter, boothId, subscribeType, topic);
    return emitter;
  }

  private void subscribeIfFirst(Long boothId, OrderSseSubscribeType type, ChannelTopic topic) {
    if (emitterFinder.findByBoothIdAndSubscribeType(boothId, type).size() == 1) {
      listenerContainer.addMessageListener(listener, topic);
    }
  }

  private void registerUnsubscribeCallback(
      SseEmitter emitter, Long boothId, OrderSseSubscribeType type, ChannelTopic topic) {

    Runnable unsubscribeIfLast =
        () -> {
          if (emitterFinder.findByBoothIdAndSubscribeType(boothId, type).isEmpty()) {
            listenerContainer.removeMessageListener(listener, topic);
          }
        };

    emitter.onCompletion(unsubscribeIfLast);
    emitter.onTimeout(unsubscribeIfLast);
    emitter.onError(e -> unsubscribeIfLast.run());
  }
}
