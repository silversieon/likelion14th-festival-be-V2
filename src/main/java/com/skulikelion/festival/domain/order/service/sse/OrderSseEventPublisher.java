/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.sse;

public interface OrderSseEventPublisher {

  void publish(Long boothId, SseSubscribeType subscribeType, String eventName, Object payload);
}
