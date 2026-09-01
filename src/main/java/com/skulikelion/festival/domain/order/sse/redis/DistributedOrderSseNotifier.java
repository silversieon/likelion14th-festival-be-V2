/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.order.dto.response.*;
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
      Booth booth, OrderSseSubscribeType currentSubscribeType, Long orderId) {
    OrderCountNotification notification = OrderCountNotification.of(currentSubscribeType, orderId);
    OrderSseSubscribeType.exclude(currentSubscribeType)
        .forEach(
            targetType ->
                publisher.publish(
                    booth.getId(),
                    targetType,
                    OrderSseEventType.ORDER_INCREMENT_NOTIFICATION.getEventName(),
                    notification));
  }

  @Override
  public void sendOrderDecrementNotification(
      Booth booth, OrderSseSubscribeType currentSubscribeType, Long orderId) {
    OrderCountNotification notification = OrderCountNotification.of(currentSubscribeType, orderId);
    OrderSseSubscribeType.exclude(currentSubscribeType)
        .forEach(
            targetType ->
                publisher.publish(
                    booth.getId(),
                    targetType,
                    OrderSseEventType.ORDER_DECREMENT_NOTIFICATION.getEventName(),
                    notification));
  }

  @Override
  public void sendOrderDismissNotification(
      Booth booth, OrderSseSubscribeType currentSubscribeType, Long orderId) {
    DismissOrderIdNotification notification = DismissOrderIdNotification.of(orderId);
    publisher.publish(
        booth.getId(),
        currentSubscribeType,
        OrderSseEventType.DISMISS_NOTIFICATION.getEventName(),
        notification);
  }

  @Override
  public void sendWaitingOrderEvent(Booth booth, WaitingOrderResponse waitingOrderResponse) {
    publisher.publish(
        booth.getId(),
        OrderSseSubscribeType.WAITING,
        OrderSseEventType.WAITING_ORDER_EVENT.getEventName(),
        waitingOrderResponse);
  }

  @Override
  public void sendCookingOrderEvent(Booth booth, CookingOrderResponse cookingOrderResponse) {
    publisher.publish(
        booth.getId(),
        OrderSseSubscribeType.COOKING,
        OrderSseEventType.COOKING_ORDER_EVENT.getEventName(),
        cookingOrderResponse);
  }

  @Override
  public void sendCompletedOrderEvent(Booth booth, CompletedOrderResponse completedOrderResponse) {
    publisher.publish(
        booth.getId(),
        OrderSseSubscribeType.COMPLETED,
        OrderSseEventType.COMPLETED_ORDER_EVENT.getEventName(),
        completedOrderResponse);
  }

  @Override
  public void sendCanceledOrderEvent(Booth booth, CanceledOrderResponse canceledOrderResponse) {
    publisher.publish(
        booth.getId(),
        OrderSseSubscribeType.CANCELED,
        OrderSseEventType.CANCELED_ORDER_EVENT.getEventName(),
        canceledOrderResponse);
  }

  @Override
  public void sendOrderItemUnitStatusEvent(
      Booth booth, OrderItemUnitStatusResponse orderItemUnitStatusResponse) {
    publisher.publish(
        booth.getId(),
        OrderSseSubscribeType.COOKING,
        OrderSseEventType.ORDER_ITEM_UNIT_STATUS_EVENT.getEventName(),
        orderItemUnitStatusResponse);
  }
}
