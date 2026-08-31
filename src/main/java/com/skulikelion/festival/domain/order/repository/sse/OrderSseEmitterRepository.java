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

import com.skulikelion.festival.domain.order.enums.SseSubscribeType;
import com.skulikelion.festival.domain.order.service.OrderService;

/**
 * 멋쟁이사자처럼 서경대학교 축제 페이지 주문 이벤트 저장소 입니다.
 *
 * @implSpec 부스 식별자, 구독 타입에 따라 별도의 SseEmitter 객체를 가집니다. 이벤트 발생 시에 각 부스의 구독 타입과 일치하는 SseEmitter(사용자)를
 *     찾아 함수를 동작시킵니다.
 * @since 2026.04.29
 * @see OrderService
 * @author Keum Si Eon
 * @version latest: 1
 */
@Component
public class OrderSseEmitterRepository {

  private final Map<Long, Map<SseSubscribeType, List<SseEmitter>>> emitters =
      new ConcurrentHashMap<>();

  public void save(Long boothId, SseSubscribeType sseSubscribeType, SseEmitter emitter) {
    emitters
        .computeIfAbsent(boothId, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(sseSubscribeType, k -> new CopyOnWriteArrayList<>())
        .add(emitter);
  }

  public Map<SseSubscribeType, List<SseEmitter>> findByBoothId(Long boothId) {
    return emitters.getOrDefault(boothId, Collections.emptyMap());
  }

  public List<SseEmitter> findByBoothIdAndSubscribeType(
      Long boothId, SseSubscribeType sseSubscribeType) {
    return emitters
        .getOrDefault(boothId, Collections.emptyMap())
        .getOrDefault(sseSubscribeType, Collections.emptyList());
  }

  public void remove(Long boothId, SseSubscribeType sseSubscribeType, SseEmitter emitter) {
    Map<SseSubscribeType, List<SseEmitter>> statusMap = emitters.get(boothId);
    if (statusMap == null) return;

    List<SseEmitter> list = statusMap.get(sseSubscribeType);
    if (list == null) return;

    list.remove(emitter);

    if (list.isEmpty()) statusMap.remove(sseSubscribeType);
    if (statusMap.isEmpty()) emitters.remove(boothId);
  }
}
