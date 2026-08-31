package com.skulikelion.festival.domain.order.service.sse;

import com.skulikelion.festival.domain.order.enums.SseSubscribeType;

public interface OrderSseEventPublisher {

    void publish(Long boothId, SseSubscribeType subscribeType, String eventName, Object payload);
}
