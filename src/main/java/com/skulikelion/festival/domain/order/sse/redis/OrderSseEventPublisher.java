/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis;

import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;

public interface OrderSseEventPublisher {

  /**
   * [ 이벤트 발행 메서드 ] 분산 환경에서 이벤트를 발행합니다.
   *
   * @param boothId 부스 식별자
   * @param subscribeType 구독 타입
   * @param eventName 이벤트명
   * @param payload 전송 데이터
   */
  void publish(Long boothId, OrderSseSubscribeType subscribeType, String eventName, Object payload);
}
