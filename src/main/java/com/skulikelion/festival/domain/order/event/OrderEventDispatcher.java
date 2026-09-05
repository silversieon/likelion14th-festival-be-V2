/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.event;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.order.event.payload.CanceledOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.CompletedOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.CookingOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.DismissOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.OrderItemUnitStatusPayload;
import com.skulikelion.festival.domain.order.event.payload.WaitingOrderPayload;
import com.skulikelion.festival.domain.order.sse.OrderSseEventType;
import com.skulikelion.festival.domain.order.sse.OrderSseNotifier;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class OrderEventDispatcher {

  private final OrderSseNotifier notifier;
  private final ObjectMapper objectMapper;

  public void handleOrderEvent(String payload, String eventName) {
    OrderSseEventType orderSseEventType = OrderSseEventType.fromEventName(eventName);
    switch (orderSseEventType) {
      case WAITING_ORDER_EVENT -> handleWaitingOrderEvent(
          objectMapper.readValue(payload, WaitingOrderPayload.class));
      case COOKING_ORDER_EVENT -> handleCookingOrderEvent(
          objectMapper.readValue(payload, CookingOrderPayload.class));
      case COMPLETED_ORDER_EVENT -> handleCompletedOrderEvent(
          objectMapper.readValue(payload, CompletedOrderPayload.class));
      case CANCELED_ORDER_EVENT -> handleCanceledOrderEvent(
          objectMapper.readValue(payload, CanceledOrderPayload.class));
      case ORDER_ITEM_UNIT_STATUS_EVENT -> handleOrderItemUnitStatusEvent(
          objectMapper.readValue(payload, OrderItemUnitStatusPayload.class));
      case DISMISS_NOTIFICATION -> handleDismissOrderEvent(
          objectMapper.readValue(payload, DismissOrderPayload.class));
    }
  }

  protected void handleWaitingOrderEvent(WaitingOrderPayload waitingOrderPayload) {
    notifier.sendWaitingOrderEvent(waitingOrderPayload);
    notifier.sendOrderIncrementNotification(
        waitingOrderPayload.boothId(),
        waitingOrderPayload.waitingStatus().toSseSubscribeType(),
        waitingOrderPayload.waitingOrderResponse().getOrderId());
  }

  protected void handleCookingOrderEvent(CookingOrderPayload cookingOrderPayload) {
    notifier.sendCookingOrderEvent(cookingOrderPayload);
    notifier.sendOrderIncrementNotification(
        cookingOrderPayload.boothId(),
        cookingOrderPayload.currentStatus().toSseSubscribeType(),
        cookingOrderPayload.cookingOrderResponse().getOrderId());
    notifier.sendOrderDecrementNotification(
        cookingOrderPayload.boothId(),
        cookingOrderPayload.previousStatus().toSseSubscribeType(),
        cookingOrderPayload.cookingOrderResponse().getOrderId());
  }

  protected void handleCompletedOrderEvent(CompletedOrderPayload completedOrderPayload) {
    notifier.sendCompletedOrderEvent(completedOrderPayload);
    notifier.sendOrderDecrementNotification(
        completedOrderPayload.boothId(),
        completedOrderPayload.previousStatus().toSseSubscribeType(),
        completedOrderPayload.completedOrderResponse().getOrderId());
  }

  protected void handleCanceledOrderEvent(CanceledOrderPayload canceledOrderPayload) {
    notifier.sendCanceledOrderEvent(canceledOrderPayload);
  }

  protected void handleOrderItemUnitStatusEvent(
      OrderItemUnitStatusPayload orderItemUnitStatusPayload) {
    notifier.sendOrderItemUnitStatusEvent(orderItemUnitStatusPayload);
  }

  protected void handleDismissOrderEvent(DismissOrderPayload dismissOrderPayload) {
    notifier.sendOrderDismissNotification(dismissOrderPayload);
  }
}
