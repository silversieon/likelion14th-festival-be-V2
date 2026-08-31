/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.skulikelion.festival.domain.order.dto.payload.CanceledOrderPayload;
import com.skulikelion.festival.domain.order.dto.payload.CompletedOrderPayload;
import com.skulikelion.festival.domain.order.dto.payload.CookingOrderPayload;
import com.skulikelion.festival.domain.order.dto.payload.DismissOrderPayload;
import com.skulikelion.festival.domain.order.dto.payload.OrderItemUnitStatusPayload;
import com.skulikelion.festival.domain.order.dto.payload.WaitingOrderPayload;
import com.skulikelion.festival.domain.order.service.OrderService;
import com.skulikelion.festival.domain.order.service.sse.OrderSseNotifier;
import com.skulikelion.festival.domain.order.service.sse.SseSubscribeType;

import lombok.RequiredArgsConstructor;

/**
 * 멋쟁이사자처럼 서경대학교 축제 페이지 주문 관련 EventListener 입니다.
 *
 * @implSpec Service 계층에서 매개변수에 맞게 함수를 호출하면 트리거가 동작합니다. Transaction 커밋, 롤백 여부에 따라 동작 여부를 결정합니다.
 * @since 2026.04.29
 * @see OrderService
 * @author Keum Si Eon
 * @version latest: 1
 */
@Component
@RequiredArgsConstructor
public class OrderEventListener {

  private final OrderSseNotifier notifier;

  @Async("eventExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleWaitingOrderEvent(WaitingOrderPayload waitingOrderPayload) {
    notifier.sendWaitingOrderEvent(
        waitingOrderPayload.booth(), waitingOrderPayload.waitingOrderResponse());
    notifier.sendOrderIncrementNotification(
        waitingOrderPayload.booth(),
        SseSubscribeType.WAITING,
        waitingOrderPayload.waitingOrderResponse().getOrderId());
  }

  @Async("eventExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCookingOrderEvent(CookingOrderPayload cookingOrderPayload) {
    notifier.sendCookingOrderEvent(
        cookingOrderPayload.booth(), cookingOrderPayload.cookingOrderResponse());
    notifier.sendOrderIncrementNotification(
        cookingOrderPayload.booth(),
        cookingOrderPayload.currentStatus().toSseSubscribeType(),
        cookingOrderPayload.cookingOrderResponse().getOrderId());
    notifier.sendOrderDecrementNotification(
        cookingOrderPayload.booth(),
        cookingOrderPayload.previousStatus().toSseSubscribeType(),
        cookingOrderPayload.cookingOrderResponse().getOrderId());
  }

  @Async("eventExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCompletedOrderEvent(CompletedOrderPayload completedOrderPayload) {
    notifier.sendCompletedOrderEvent(
        completedOrderPayload.booth(), completedOrderPayload.completedOrderResponse());
    notifier.sendOrderDecrementNotification(
        completedOrderPayload.booth(),
        completedOrderPayload.previousStatus().toSseSubscribeType(),
        completedOrderPayload.completedOrderResponse().getOrderId());
  }

  @Async("eventExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCanceledOrderEvent(CanceledOrderPayload canceledOrderPayload) {
    notifier.sendCanceledOrderEvent(
        canceledOrderPayload.booth(), canceledOrderPayload.canceledOrderResponse());
  }

  @Async("eventExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderItemUnitStatusEvent(
      OrderItemUnitStatusPayload orderItemUnitStatusPayload) {
    notifier.sendOrderItemUnitStatusEvent(
        orderItemUnitStatusPayload.booth(),
        orderItemUnitStatusPayload.orderItemUnitStatusResponse());
  }

  @Async("eventExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleDismissOrderEvent(DismissOrderPayload dismissOrderPayload) {
    notifier.sendOrderDismissNotification(
        dismissOrderPayload.booth(),
        dismissOrderPayload.currentOrderStatus().toSseSubscribeType(),
        dismissOrderPayload.orderId());
  }
}
