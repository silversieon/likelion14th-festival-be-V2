/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.skulikelion.festival.domain.order.dto.event.CanceledOrderPayload;
import com.skulikelion.festival.domain.order.dto.event.CompletedOrderPayload;
import com.skulikelion.festival.domain.order.dto.event.CookingOrderItemUnitPayload;
import com.skulikelion.festival.domain.order.dto.event.CookingOrderPayload;
import com.skulikelion.festival.domain.order.dto.event.WaitingOrderPayload;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;
import com.skulikelion.festival.domain.order.service.sse.OrderSseService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderEventListener {

  private final OrderSseService orderSseService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleWaitingOrderEvent(WaitingOrderPayload waitingOrderPayload) {
    orderSseService.sendWaitingOrderEvent(
        waitingOrderPayload.booth(), waitingOrderPayload.waitingOrderResponse());
    orderSseService.sendOrderEventNotification(waitingOrderPayload.booth(), OrderStatus.WAITING);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCookingOrderEvent(CookingOrderPayload cookingOrderPayload) {
    orderSseService.sendCookingOrderEvent(
        cookingOrderPayload.booth(), cookingOrderPayload.cookingOrderResponse());
    orderSseService.sendOrderEventNotification(cookingOrderPayload.booth(), OrderStatus.COOKING);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCompletedOrderEvent(CompletedOrderPayload completedOrderPayload) {
    orderSseService.sendCompletedOrderEvent(
        completedOrderPayload.booth(), completedOrderPayload.completedOrderResponse());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCanceledOrderEvent(CanceledOrderPayload canceledOrderPayload) {
    orderSseService.sendCanceledOrderEvent(
        canceledOrderPayload.booth(), canceledOrderPayload.canceledOrderResponse());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCookingOrderItemUnitEvent(
      CookingOrderItemUnitPayload cookingOrderItemUnitPayload) {
    orderSseService.sendCookingOrderItemUnitEvent(
        cookingOrderItemUnitPayload.booth(),
        cookingOrderItemUnitPayload.cookingOrderItemUnitResponse());
  }
}
