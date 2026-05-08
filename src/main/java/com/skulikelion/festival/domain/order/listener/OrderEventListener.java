/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.skulikelion.festival.domain.order.dto.payload.CanceledOrderPayload;
import com.skulikelion.festival.domain.order.dto.payload.CompletedOrderPayload;
import com.skulikelion.festival.domain.order.dto.payload.CookingOrderPayload;
import com.skulikelion.festival.domain.order.dto.payload.DismissOrderPayload;
import com.skulikelion.festival.domain.order.dto.payload.OrderIdempotencyPayload;
import com.skulikelion.festival.domain.order.dto.payload.OrderItemUnitStatusPayload;
import com.skulikelion.festival.domain.order.dto.payload.WaitingOrderPayload;
import com.skulikelion.festival.domain.order.enums.SseSubscribeType;
import com.skulikelion.festival.domain.order.service.OrderService;
import com.skulikelion.festival.domain.order.service.idempotency.OrderIdempotencyService;
import com.skulikelion.festival.domain.order.service.sse.OrderSseService;

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

  private final OrderSseService orderSseService;
  private final OrderIdempotencyService orderIdempotencyService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleWaitingOrderEvent(WaitingOrderPayload waitingOrderPayload) {
    orderSseService.sendWaitingOrderEvent(
        waitingOrderPayload.booth(), waitingOrderPayload.waitingOrderResponse());
    orderSseService.sendOrderIncrementNotification(
        waitingOrderPayload.booth(),
        SseSubscribeType.WAITING,
        waitingOrderPayload.waitingOrderResponse().getOrderId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCookingOrderEvent(CookingOrderPayload cookingOrderPayload) {
    orderSseService.sendCookingOrderEvent(
        cookingOrderPayload.booth(), cookingOrderPayload.cookingOrderResponse());
    orderSseService.sendOrderIncrementNotification(
        cookingOrderPayload.booth(),
        cookingOrderPayload.currentStatus().toSseSubscribeType(),
        cookingOrderPayload.cookingOrderResponse().getOrderId());
    orderSseService.sendOrderDecrementNotification(
        cookingOrderPayload.booth(),
        cookingOrderPayload.previousStatus().toSseSubscribeType(),
        cookingOrderPayload.cookingOrderResponse().getOrderId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCompletedOrderEvent(CompletedOrderPayload completedOrderPayload) {
    orderSseService.sendCompletedOrderEvent(
        completedOrderPayload.booth(), completedOrderPayload.completedOrderResponse());
    orderSseService.sendOrderDecrementNotification(
        completedOrderPayload.booth(),
        completedOrderPayload.previousStatus().toSseSubscribeType(),
        completedOrderPayload.completedOrderResponse().getOrderId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCanceledOrderEvent(CanceledOrderPayload canceledOrderPayload) {
    orderSseService.sendCanceledOrderEvent(
        canceledOrderPayload.booth(), canceledOrderPayload.canceledOrderResponse());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderItemUnitStatusEvent(
      OrderItemUnitStatusPayload orderItemUnitStatusPayload) {
    orderSseService.sendOrderItemUnitStatusEvent(
        orderItemUnitStatusPayload.booth(),
        orderItemUnitStatusPayload.orderItemUnitStatusResponse());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleDismissOrderEvent(DismissOrderPayload dismissOrderPayload) {
    orderSseService.sendOrderDismissNotification(
        dismissOrderPayload.booth(),
        dismissOrderPayload.currentOrderStatus().toSseSubscribeType(),
        dismissOrderPayload.orderId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleIdempotentOrderEvent(OrderIdempotencyPayload orderIdempotencyPayload) {
    try {
      orderIdempotencyService.saveResponse(
          orderIdempotencyPayload.idempotencyKey(), orderIdempotencyPayload.orderResponse());
    } catch (Exception e) {
      orderIdempotencyService.deleteKey(orderIdempotencyPayload.idempotencyKey());
    }
  }
}
