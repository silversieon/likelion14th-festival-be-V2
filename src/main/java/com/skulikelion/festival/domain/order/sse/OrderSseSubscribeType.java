/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "SSE 구독 타입")
public enum OrderSseSubscribeType {
  WAITING,
  COOKING,
  COMPLETED,
  CANCELED,
  PRODUCT;

  public static List<OrderSseSubscribeType> exclude(OrderSseSubscribeType excluded) {
    return Arrays.stream(values()).filter(type -> type != excluded).toList();
  }

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
