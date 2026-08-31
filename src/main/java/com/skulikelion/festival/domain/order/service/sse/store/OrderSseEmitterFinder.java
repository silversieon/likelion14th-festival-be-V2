/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.sse.store;

import java.util.List;
import java.util.Map;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.order.service.sse.SseSubscribeType;

public interface OrderSseEmitterFinder {

  /**
   * [ 부스 내 모든 Emitter 조회 ] 부스 식별자로 구독 타입과 상관없이 등록된 모든 Emitter 조회
   *
   * @param boothId 부스 식별자
   * @return 각 구독 타입별 모든 Emitter 객체
   */
  Map<SseSubscribeType, List<SseEmitter>> findByBoothId(Long boothId);

  /**
   * [ 부스 내 특정 구독 타입 Emitter 조회 ] 부스 식별자와 구독 타입으로 등록된 모든 Emitter 조회
   *
   * @param boothId 부스 식별자
   * @param sseSubscribeType 구독 타입 (주문 상태)
   * @return 특정 구독 타입의 모든 Emitter 객체
   */
  List<SseEmitter> findByBoothIdAndSubscribeType(Long boothId, SseSubscribeType sseSubscribeType);
}
