/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface OrderSseSubscriber {

  /**
   * [ 주문 상태 구독 ] 학과명과 구독 타입을 통해 특정 주문 상태를 구독
   *
   * @param departmentName 학과명
   * @param subscribeType 구독 타입(주문 상태)
   * @return SseEmitter 객체
   */
  SseEmitter subscribeOrderStatus(String departmentName, SseSubscribeType subscribeType);
}
