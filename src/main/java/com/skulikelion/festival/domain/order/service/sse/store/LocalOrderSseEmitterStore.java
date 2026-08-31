/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.sse.store;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.order.service.sse.SseSubscribeType;

@Component
public class LocalOrderSseEmitterStore implements OrderSseEmitterRegistry, OrderSseEmitterFinder {

  private final Map<Long, Map<SseSubscribeType, List<SseEmitter>>> emitters =
      new ConcurrentHashMap<>();

  @Override
  public void register(Long boothId, SseSubscribeType subscribeType, SseEmitter emitter) {
    emitters
        .computeIfAbsent(boothId, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(subscribeType, k -> new CopyOnWriteArrayList<>())
        .add(emitter);
  }

  @Override
  public void remove(Long boothId, SseSubscribeType subscribeType, SseEmitter emitter) {
    Map<SseSubscribeType, List<SseEmitter>> subscribeTypeMap = emitters.get(boothId);
    if (subscribeTypeMap == null) return;

    List<SseEmitter> emitterList = subscribeTypeMap.get(subscribeType);
    if (emitterList == null) return;

    emitterList.remove(emitter);
    if (emitterList.isEmpty()) subscribeTypeMap.remove(subscribeType);
    if (subscribeTypeMap.isEmpty()) emitters.remove(boothId);
  }

  @Override
  public Map<SseSubscribeType, List<SseEmitter>> findByBoothId(Long boothId) {
    return emitters.getOrDefault(boothId, Collections.emptyMap());
  }

  @Override
  public List<SseEmitter> findByBoothIdAndSubscribeType(
      Long boothId, SseSubscribeType sseSubscribeType) {
    return emitters
        .getOrDefault(boothId, Collections.emptyMap())
        .getOrDefault(sseSubscribeType, Collections.emptyList());
  }
}
