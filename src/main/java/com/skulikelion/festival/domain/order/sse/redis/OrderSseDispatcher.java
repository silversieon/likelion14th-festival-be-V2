/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;
import com.skulikelion.festival.domain.order.sse.dto.SseEventMessage;
import com.skulikelion.festival.global.security.AuthPrincipal;

public interface OrderSseDispatcher {

  void dispatch(OrderSseChannelInfo info, SseEventMessage eventMessage, String recordId);

  default void replay(
      AuthPrincipal authPrincipal,
      OrderSseSubscribeType subscribeType,
      String lastEventId,
      SseEmitter emitter) {}
}
