/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.event.listener;

import org.springframework.context.event.EventListener;

import com.skulikelion.festival.domain.order.event.payload.CanceledOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.CompletedOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.CookingOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.DismissOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.OrderItemUnitStatusPayload;
import com.skulikelion.festival.domain.order.event.payload.WaitingOrderPayload;
import com.skulikelion.festival.domain.order.sse.OrderSseEventType;
import com.skulikelion.festival.global.outbox.AggregateType;
import com.skulikelion.festival.global.outbox.Outbox;
import com.skulikelion.festival.global.outbox.OutboxRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
public class OrderOutboxEventListener implements OrderEventListener {

  private final OutboxRepository outboxRepository;
  private final ObjectMapper objectMapper;
  private static final AggregateType AGGREGATE_TYPE = AggregateType.ORDER;

  // 메서드들이 반드시 동기적으로 실행되어 동일 트랜잭션에서의 Outbox 패턴 보장 필요

  @Override
  @EventListener
  public void handleWaitingOrderEvent(WaitingOrderPayload payload) {
    outboxRepository.save(
        Outbox.of(
            payload.waitingOrderResponse().getOrderId().toString(),
            AGGREGATE_TYPE,
            OrderSseEventType.WAITING_ORDER_EVENT.getEventName(),
            objectMapper.writeValueAsString(payload)));
    outboxRepository.save(
        Outbox.of(
            payload.boothId().toString(),
            AGGREGATE_TYPE,
            OrderSseEventType.ORDER_INCREMENT_NOTIFICATION.getEventName(),
            objectMapper.writeValueAsString(payload)));
  }

  @Override
  @EventListener
  public void handleCookingOrderEvent(CookingOrderPayload payload) {
    outboxRepository.save(
        Outbox.of(
            payload.cookingOrderResponse().getOrderId().toString(),
            AGGREGATE_TYPE,
            OrderSseEventType.COOKING_ORDER_EVENT.getEventName(),
            objectMapper.writeValueAsString(payload)));
    outboxRepository.save(
        Outbox.of(
            payload.boothId().toString(),
            AGGREGATE_TYPE,
            OrderSseEventType.ORDER_INCREMENT_NOTIFICATION.getEventName(),
            objectMapper.writeValueAsString(payload)));
    outboxRepository.save(
        Outbox.of(
            payload.boothId().toString(),
            AGGREGATE_TYPE,
            OrderSseEventType.ORDER_DECREMENT_NOTIFICATION.getEventName(),
            objectMapper.writeValueAsString(payload)));
  }

  @Override
  @EventListener
  public void handleCompletedOrderEvent(CompletedOrderPayload payload) {
    outboxRepository.save(
        Outbox.of(
            payload.completedOrderResponse().getOrderId().toString(),
            AGGREGATE_TYPE,
            OrderSseEventType.COMPLETED_ORDER_EVENT.getEventName(),
            objectMapper.writeValueAsString(payload)));
    outboxRepository.save(
        Outbox.of(
            payload.boothId().toString(),
            AGGREGATE_TYPE,
            OrderSseEventType.ORDER_DECREMENT_NOTIFICATION.getEventName(),
            objectMapper.writeValueAsString(payload)));
  }

  @Override
  @EventListener
  public void handleCanceledOrderEvent(CanceledOrderPayload payload) {
    outboxRepository.save(
        Outbox.of(
            payload.boothId().toString(),
            AGGREGATE_TYPE,
            OrderSseEventType.CANCELED_ORDER_EVENT.getEventName(),
            objectMapper.writeValueAsString(payload)));
  }

  @Override
  @EventListener
  public void handleOrderItemUnitStatusEvent(OrderItemUnitStatusPayload payload) {
    outboxRepository.save(
        Outbox.of(
            payload.orderItemUnitStatusResponse().getOrderItemUnitId().toString(),
            AGGREGATE_TYPE,
            OrderSseEventType.ORDER_ITEM_UNIT_STATUS_EVENT.getEventName(),
            objectMapper.writeValueAsString(payload)));
  }

  @Override
  @EventListener
  public void handleDismissOrderEvent(DismissOrderPayload payload) {
    outboxRepository.save(
        Outbox.of(
            payload.orderId().toString(),
            AGGREGATE_TYPE,
            OrderSseEventType.DISMISS_NOTIFICATION.getEventName(),
            objectMapper.writeValueAsString(payload)));
  }
}
