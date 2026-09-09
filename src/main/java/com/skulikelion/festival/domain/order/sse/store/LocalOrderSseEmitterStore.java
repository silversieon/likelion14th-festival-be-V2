/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;

@Component
public class LocalOrderSseEmitterStore implements OrderSseEmitterRegistry, OrderSseEmitterFinder {

  private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

  private String key(Long boothId, OrderSseSubscribeType type) {
    return boothId + ":" + type;
  }

  @Override
  public void register(Long boothId, OrderSseSubscribeType subscribeType, SseEmitter emitter) {
    emitters.compute(
        key(boothId, subscribeType),
        (k, list) -> {
          if (list == null) {
            list = new ArrayList<>();
          }
          list.add(emitter);
          return list;
        });
  }

  @Override
  public void remove(Long boothId, OrderSseSubscribeType subscribeType, SseEmitter emitter) {
    emitters.computeIfPresent(
        key(boothId, subscribeType),
        (k, list) -> {
          list.remove(emitter);
          return list.isEmpty() ? null : list;
        });
  }

  @Override
  public boolean registerAndCheckFirst(
      Long boothId, OrderSseSubscribeType subscribeType, SseEmitter emitter) {

    AtomicBoolean wasFirst = new AtomicBoolean(false);

    emitters.compute(
        key(boothId, subscribeType),
        (k, list) -> {
          if (list == null) {
            list = new ArrayList<>();
            wasFirst.set(true);
          }
          list.add(emitter);
          return list;
        });

    return wasFirst.get();
  }

  @Override
  public boolean removeAndCheckLast(
      Long boothId, OrderSseSubscribeType subscribeType, SseEmitter emitter) {

    AtomicBoolean wasLast = new AtomicBoolean(false);

    emitters.computeIfPresent(
        key(boothId, subscribeType),
        (k, list) -> {
          list.remove(emitter);
          if (list.isEmpty()) {
            wasLast.set(true);
            return null;
          }
          return list;
        });

    return wasLast.get();
  }

  @Override
  public List<SseEmitter> findByBoothIdAndSubscribeType(Long boothId, OrderSseSubscribeType type) {
    return emitters.getOrDefault(key(boothId, type), Collections.emptyList());
  }

  @Override
  public Map<OrderSseSubscribeType, List<SseEmitter>> findByBoothId(Long boothId) {
    return emitters.entrySet().stream()
        .filter(e -> e.getKey().startsWith(boothId + ":"))
        .collect(
            Collectors.toMap(
                e -> OrderSseSubscribeType.valueOf(e.getKey().split(":")[1]), Map.Entry::getValue));
  }
}
