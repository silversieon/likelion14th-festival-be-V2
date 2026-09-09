/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.store;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;

public interface OrderSseEmitterRegistry {

  /**
   * [ Emitter 등록 ] 부스 식별자, 구독 타입으로 Emitter 객체를 등록
   *
   * @param boothId 부스 식별자
   * @param subscribeType 구독 타입 (주문 상태)
   * @param emitter Emitter 객체
   */
  void register(Long boothId, OrderSseSubscribeType subscribeType, SseEmitter emitter);

  /**
   * [ Emitter 제거 ] 부스 식별자, 구독 타입으로 Emitter 객체 제거
   *
   * @param boothId 부스 식별자
   * @param subscribeType 구독 타입 (주문 상태)
   * @param emitter Emitter 객체
   */
  void remove(Long boothId, OrderSseSubscribeType subscribeType, SseEmitter emitter);

  boolean registerAndCheckFirst(
      Long boothId, OrderSseSubscribeType subscribeType, SseEmitter emitter);

  boolean removeAndCheckLast(Long boothId, OrderSseSubscribeType subscribeType, SseEmitter emitter);
}
