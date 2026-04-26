/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
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
      return false;
    }
  };

  private final String description;

  public abstract boolean canChangeTo(OrderStatus orderStatus);
}
