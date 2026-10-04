/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis.pubsub;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.booth.service.booth.BoothService;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscriber;
import com.skulikelion.festival.domain.order.sse.redis.OrderSseChannelResolver;
import com.skulikelion.festival.domain.order.sse.store.OrderSseEmitterRegistry;
import com.skulikelion.festival.global.security.AuthPrincipal;

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

  private final Map<String, ReentrantLock> channelLocks = new ConcurrentHashMap<>();

  private ReentrantLock lockFor(String channel) {
    return channelLocks.computeIfAbsent(channel, k -> new ReentrantLock());
  }

  @Override
  public SseEmitter subscribeOrderStatus(
      AuthPrincipal principal, OrderSseSubscribeType subscribeType) {
    Long boothId = boothService.getRequiredBooth(principal).getId();
    String channel = channelResolver.toChannel(boothId, subscribeType);
    ChannelTopic topic = new ChannelTopic(channel);
    SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);

    emitter.onCompletion(() -> unsubscribe(emitter, boothId, subscribeType, channel, topic));
    emitter.onTimeout(emitter::complete);
    emitter.onError(e -> emitter.complete());

    ReentrantLock lock = lockFor(channel);
    lock.lock();
    try {
      boolean isFirst = registry.registerAndCheckFirst(boothId, subscribeType, emitter);
      if (isFirst) {
        try {
          listenerContainer.addMessageListener(listener, topic);
        } catch (RuntimeException e) {
          registry.remove(boothId, subscribeType, emitter); // 등록 롤백
          throw e;
        }
      }
    } finally {
      lock.unlock();
    }

    connectCheck(emitter);
    return emitter;
  }

  private void unsubscribe(
      SseEmitter emitter,
      Long boothId,
      OrderSseSubscribeType subscribeType,
      String channel,
      ChannelTopic topic) {
    ReentrantLock lock = lockFor(channel);
    lock.lock();
    try {
      boolean isLast = registry.removeAndCheckLast(boothId, subscribeType, emitter);
      if (isLast) {
        listenerContainer.removeMessageListener(listener, topic); // 이 토픽에서만 제거
      }
    } finally {
      lock.unlock();
    }
  }

  private void connectCheck(SseEmitter emitter) {
    try {
      emitter.send(SseEmitter.event().name("connect").data("connected order subscribe"));
    } catch (IOException e) {
      emitter.completeWithError(e);
    }
  }
}
