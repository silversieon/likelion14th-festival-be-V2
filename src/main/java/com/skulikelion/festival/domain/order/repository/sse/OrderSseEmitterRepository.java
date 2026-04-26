/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.repository.sse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;

@Component
public class OrderSseEmitterRepository {

  private final Map<Long, Map<OrderStatus, List<SseEmitter>>> emitters = new ConcurrentHashMap<>();

  public void save(Long boothId, OrderStatus orderStatus, SseEmitter emitter) {
    emitters
        .computeIfAbsent(boothId, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(orderStatus, k -> new CopyOnWriteArrayList<>())
        .add(emitter);
  }

  public Map<OrderStatus, List<SseEmitter>> findByBoothId(Long boothId) {
    return emitters.getOrDefault(boothId, Collections.emptyMap());
  }

  public List<SseEmitter> findByBoothIdAndOrderStatus(Long boothId, OrderStatus orderStatus) {
    return emitters
        .getOrDefault(boothId, Collections.emptyMap())
        .getOrDefault(orderStatus, Collections.emptyList());
  }

  public void remove(Long boothId, OrderStatus orderStatus, SseEmitter emitter) {
    Map<OrderStatus, List<SseEmitter>> statusMap = emitters.get(boothId);
    if (statusMap == null) return;

    List<SseEmitter> list = statusMap.get(orderStatus);
    if (list == null) return;

    list.remove(emitter);

    if (list.isEmpty()) statusMap.remove(orderStatus);
    if (statusMap.isEmpty()) emitters.remove(boothId);
  }
}
