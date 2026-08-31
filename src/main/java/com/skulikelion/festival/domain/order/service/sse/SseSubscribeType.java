/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.sse;

import java.util.Optional;

import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "SSE 구독 타입")
public enum SseSubscribeType {
  WAITING,
  COOKING,
  COMPLETED,
  CANCELED,
  PRODUCT;

  public Optional<OrderStatus> toOrderStatus() {
    return switch (this) {
      case WAITING -> Optional.of(OrderStatus.WAITING);
      case COOKING -> Optional.of(OrderStatus.COOKING);
      case COMPLETED -> Optional.of(OrderStatus.COMPLETED);
      case CANCELED -> Optional.of(OrderStatus.CANCELED);
      case PRODUCT -> Optional.empty();
    };
  }
}
