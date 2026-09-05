/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis;

import com.skulikelion.festival.domain.order.event.payload.CanceledOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.CompletedOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.CookingOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.DismissOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.OrderItemUnitStatusPayload;
import com.skulikelion.festival.domain.order.event.payload.WaitingOrderPayload;
import com.skulikelion.festival.domain.order.sse.OrderSseEventType;
import com.skulikelion.festival.domain.order.sse.OrderSseNotifier;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;
import com.skulikelion.festival.domain.order.sse.dto.DismissOrderIdNotification;
import com.skulikelion.festival.domain.order.sse.dto.OrderCountNotification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DistributedOrderSseNotifier implements OrderSseNotifier {

  private final OrderSseEventPublisher publisher;

  @Override
  public void sendOrderIncrementNotification(
      Long boothId, OrderSseSubscribeType currentSubscribeType, Long orderId) {
    OrderCountNotification notification = OrderCountNotification.of(currentSubscribeType, orderId);
    OrderSseSubscribeType.exclude(currentSubscribeType)
        .forEach(
            targetType ->
                publisher.publish(
                    boothId,
                    targetType,
                    OrderSseEventType.ORDER_INCREMENT_NOTIFICATION.getEventName(),
                    notification));
  }

  @Override
  public void sendOrderDecrementNotification(
      Long boothId, OrderSseSubscribeType currentSubscribeType, Long orderId) {
    OrderCountNotification notification = OrderCountNotification.of(currentSubscribeType, orderId);
    OrderSseSubscribeType.exclude(currentSubscribeType)
        .forEach(
            targetType ->
                publisher.publish(
                    boothId,
                    targetType,
                    OrderSseEventType.ORDER_DECREMENT_NOTIFICATION.getEventName(),
                    notification));
  }

  @Override
  public void sendOrderDismissNotification(DismissOrderPayload payload) {
    publisher.publish(
        payload.boothId(),
        payload.currentOrderStatus().toSseSubscribeType(),
        OrderSseEventType.DISMISS_NOTIFICATION.getEventName(),
        DismissOrderIdNotification.of(payload.orderId()));
  }

  @Override
  public void sendWaitingOrderEvent(WaitingOrderPayload payload) {
    publisher.publish(
        payload.boothId(),
        OrderSseSubscribeType.WAITING,
        OrderSseEventType.WAITING_ORDER_EVENT.getEventName(),
        payload.waitingOrderResponse());
  }

  @Override
  public void sendCookingOrderEvent(CookingOrderPayload payload) {
    publisher.publish(
        payload.boothId(),
        OrderSseSubscribeType.COOKING,
        OrderSseEventType.COOKING_ORDER_EVENT.getEventName(),
        payload.cookingOrderResponse());
  }

  @Override
  public void sendCompletedOrderEvent(CompletedOrderPayload payload) {
    publisher.publish(
        payload.boothId(),
        OrderSseSubscribeType.COMPLETED,
        OrderSseEventType.COMPLETED_ORDER_EVENT.getEventName(),
        payload.completedOrderResponse());
  }

  @Override
  public void sendCanceledOrderEvent(CanceledOrderPayload payload) {
    publisher.publish(
        payload.boothId(),
        OrderSseSubscribeType.CANCELED,
        OrderSseEventType.CANCELED_ORDER_EVENT.getEventName(),
        payload.canceledOrderResponse());
  }

  @Override
  public void sendOrderItemUnitStatusEvent(OrderItemUnitStatusPayload payload) {
    publisher.publish(
        payload.boothId(),
        OrderSseSubscribeType.COOKING,
        OrderSseEventType.ORDER_ITEM_UNIT_STATUS_EVENT.getEventName(),
        payload.orderItemUnitStatusResponse());
  }
}
