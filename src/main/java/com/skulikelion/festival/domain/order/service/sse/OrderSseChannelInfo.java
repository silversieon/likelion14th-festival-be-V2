package com.skulikelion.festival.domain.order.service.sse;

import com.skulikelion.festival.domain.order.enums.SseSubscribeType;

public record OrderSseChannelInfo(Long boothId, SseSubscribeType subscribeType) {
}
