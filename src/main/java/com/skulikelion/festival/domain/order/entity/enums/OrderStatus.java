/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.entity.enums;

import com.skulikelion.festival.domain.order.enums.SseSubscribeType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "주문 상태")
public enum OrderStatus {
  WAITING("대기중") {
    @Override
    public boolean canChangeTo(OrderStatus orderStatus) {
      return orderStatus == COOKING || orderStatus == CANCELED;
    }
  },
  COOKING("조리중") {
    @Override
    public boolean canChangeTo(OrderStatus orderStatus) {
      return orderStatus == COMPLETED || orderStatus == CANCELED;
    }
  },
  COMPLETED("완료된") {
    @Override
    public boolean canChangeTo(OrderStatus orderStatus) {
      return orderStatus == COOKING;
    }
  },
  CANCELED("취소된") {
    @Override
    public boolean canChangeTo(OrderStatus orderStatus) {
      return orderStatus == WAITING;
    }
  };

  private final String description;

  public abstract boolean canChangeTo(OrderStatus orderStatus);

  public SseSubscribeType toSseSubscribeType() {
    return switch (this) {
      case WAITING -> SseSubscribeType.WAITING;
      case COOKING -> SseSubscribeType.COOKING;
      case COMPLETED -> SseSubscribeType.COMPLETED;
      case CANCELED -> SseSubscribeType.CANCELED;
    };
  }
}
