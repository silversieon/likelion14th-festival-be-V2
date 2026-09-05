/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderSseEventType {
  ORDER_INCREMENT_NOTIFICATION("orderIncrementNotification"),
  ORDER_DECREMENT_NOTIFICATION("orderDecrementNotification"),
  DISMISS_NOTIFICATION("dismissNotification"),
  WAITING_ORDER_EVENT("waitingOrderEvent"),
  COOKING_ORDER_EVENT("cookingOrderEvent"),
  COMPLETED_ORDER_EVENT("completedOrderEvent"),
  CANCELED_ORDER_EVENT("canceledOrderEvent"),
  ORDER_ITEM_UNIT_STATUS_EVENT("orderItemUnitStatusEvent");

  private final String eventName;

  public static OrderSseEventType fromEventName(String eventName) {
    return Arrays.stream(values())
        .filter(type -> type.eventName.equals(eventName))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("알 수 없는 이벤트 타입: " + eventName));
  }
}
