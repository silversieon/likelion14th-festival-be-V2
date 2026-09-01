/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.store;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;

@Component
public class LocalOrderSseEmitterStore implements OrderSseEmitterRegistry, OrderSseEmitterFinder {

  private final Map<Long, Map<OrderSseSubscribeType, List<SseEmitter>>> emitters =
      new ConcurrentHashMap<>();

  @Override
  public void register(Long boothId, OrderSseSubscribeType subscribeType, SseEmitter emitter) {
    emitters
        .computeIfAbsent(boothId, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(subscribeType, k -> new CopyOnWriteArrayList<>())
        .add(emitter);
  }

  @Override
  public void remove(Long boothId, OrderSseSubscribeType subscribeType, SseEmitter emitter) {
    Map<OrderSseSubscribeType, List<SseEmitter>> subscribeTypeMap = emitters.get(boothId);
    if (subscribeTypeMap == null) return;

    List<SseEmitter> emitterList = subscribeTypeMap.get(subscribeType);
    if (emitterList == null) return;

    emitterList.remove(emitter);
    if (emitterList.isEmpty()) subscribeTypeMap.remove(subscribeType);
    if (subscribeTypeMap.isEmpty()) emitters.remove(boothId);
  }

  @Override
  public Map<OrderSseSubscribeType, List<SseEmitter>> findByBoothId(Long boothId) {
    return emitters.getOrDefault(boothId, Collections.emptyMap());
  }

  @Override
  public List<SseEmitter> findByBoothIdAndSubscribeType(
      Long boothId, OrderSseSubscribeType orderSseSubscribeType) {
    return emitters
        .getOrDefault(boothId, Collections.emptyMap())
        .getOrDefault(orderSseSubscribeType, Collections.emptyList());
  }
}
