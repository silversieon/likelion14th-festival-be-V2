/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.event.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.skulikelion.festival.domain.order.event.OrderEventDispatcher;
import com.skulikelion.festival.domain.order.event.payload.CanceledOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.CompletedOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.CookingOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.DismissOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.OrderItemUnitStatusPayload;
import com.skulikelion.festival.domain.order.event.payload.WaitingOrderPayload;
import com.skulikelion.festival.domain.order.service.OrderService;
import com.skulikelion.festival.domain.order.sse.OrderSseNotifier;

import tools.jackson.databind.ObjectMapper;

/**
 * 멋쟁이사자처럼 서경대학교 축제 페이지 주문 관련 EventListener 입니다.
 *
 * @implSpec Service 계층에서 매개변수에 맞게 함수를 호출하면 트리거가 동작합니다. Transaction 커밋, 롤백 여부에 따라 동작 여부를 결정합니다.
 * @since 2026.04.29
 * @see OrderService
 * @author Keum Si Eon
 * @version latest: 1
 */
public class OrderDirectEventListener extends OrderEventDispatcher implements OrderEventListener {

  public OrderDirectEventListener(OrderSseNotifier notifier, ObjectMapper objectMapper) {
    super(notifier, objectMapper);
  }

  // 비동기 실행

  @Async("eventExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleWaitingOrderEvent(WaitingOrderPayload waitingOrderPayload) {
    super.handleWaitingOrderEvent(waitingOrderPayload);
  }

  @Async("eventExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCookingOrderEvent(CookingOrderPayload cookingOrderPayload) {
    super.handleCookingOrderEvent(cookingOrderPayload);
  }

  @Async("eventExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCompletedOrderEvent(CompletedOrderPayload completedOrderPayload) {
    super.handleCompletedOrderEvent(completedOrderPayload);
  }

  @Async("eventExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCanceledOrderEvent(CanceledOrderPayload canceledOrderPayload) {
    super.handleCanceledOrderEvent(canceledOrderPayload);
  }

  @Async("eventExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderItemUnitStatusEvent(
      OrderItemUnitStatusPayload orderItemUnitStatusPayload) {
    super.handleOrderItemUnitStatusEvent(orderItemUnitStatusPayload);
  }

  @Async("eventExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleDismissOrderEvent(DismissOrderPayload dismissOrderPayload) {
    super.handleDismissOrderEvent(dismissOrderPayload);
  }
}
