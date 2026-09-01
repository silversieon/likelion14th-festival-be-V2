/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.dto;

import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;
import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;
import com.skulikelion.festival.global.exception.CustomException;

import lombok.Builder;

@Builder
public record OrderCountNotification(OrderStatus orderStatus, Long orderId) {

  public static OrderCountNotification of(OrderSseSubscribeType subscribeType, Long orderId) {
    return OrderCountNotification.builder()
        .orderStatus(
            subscribeType
                .toOrderStatus()
                .orElseThrow(
                    () -> new CustomException(OrderErrorCode.INVALID_SUBSCRIBE_TYPE_CONVERSION)))
        .orderId(orderId)
        .build();
  }
}
