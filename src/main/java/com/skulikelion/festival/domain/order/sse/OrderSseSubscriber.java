/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.global.security.AuthPrincipal;

public interface OrderSseSubscriber {

  /**
   * [ 주문 상태 구독 ] 학과명과 구독 타입을 통해 특정 주문 상태를 구독
   *
   * @param principal 인증된 요청 주체
   * @param subscribeType 구독 타입(주문 상태)
   * @return SseEmitter 객체
   */
  SseEmitter subscribeOrderStatus(AuthPrincipal principal, OrderSseSubscribeType subscribeType);
}
