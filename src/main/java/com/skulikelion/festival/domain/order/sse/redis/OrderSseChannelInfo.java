/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis;

import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;

public record OrderSseChannelInfo(Long boothId, OrderSseSubscribeType subscribeType) {}
