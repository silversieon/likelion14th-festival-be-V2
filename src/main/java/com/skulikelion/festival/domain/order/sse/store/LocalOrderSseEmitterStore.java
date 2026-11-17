/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.store;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
            list = new CopyOnWriteArrayList<>();
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
            list = new CopyOnWriteArrayList<>();
            wasFirst.set(true);
          }
          list.add(emitter);
          log.debug(
              "emitter 등록 부스 식별자: {}, 주문 상태: {}, 현재 시각: {}",
              boothId,
              subscribeType.name(),
              System.currentTimeMillis());
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
          log.debug(
              "[SseEmitter 제거] 부스 식별자: {}, 주문 상태: {}, 현재 시각: {}",
              boothId,
              subscribeType.name(),
              System.currentTimeMillis());
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
  public Map<String, List<SseEmitter>> findAll() {
    return emitters;
  }

  @Override
  public Map<OrderSseSubscribeType, List<SseEmitter>> findByBoothId(Long boothId) {
    Map<OrderSseSubscribeType, List<SseEmitter>> result =
        new EnumMap<>(OrderSseSubscribeType.class);
    for (OrderSseSubscribeType type : OrderSseSubscribeType.values()) {
      List<SseEmitter> list = emitters.get(key(boothId, type));
      if (list != null) result.put(type, list);
    }
    return result;
  }
}
