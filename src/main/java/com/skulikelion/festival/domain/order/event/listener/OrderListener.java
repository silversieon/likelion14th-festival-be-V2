/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.event.listener;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.order.dto.response.CompletedOrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderItemUnitResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderItemResponse;
import com.skulikelion.festival.domain.order.entity.Order;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;
import com.skulikelion.festival.domain.order.event.payload.OrderStatusChangedEvent;
import com.skulikelion.festival.domain.order.mapper.OrderEventMapper;
import com.skulikelion.festival.domain.order.mapper.OrderMapper;
import com.skulikelion.festival.domain.order.repository.OrderItemRepository;
import com.skulikelion.festival.domain.order.repository.OrderItemUnitRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderListener {

  private final OrderItemRepository orderItemRepository;
  private final OrderItemUnitRepository orderItemUnitRepository;
  private final OrderMapper orderMapper;
  private final OrderEventMapper orderEventMapper;
  private final ApplicationEventPublisher eventPublisher;

  // 반드시 동기 실행되어 호출 트랜잭션 내부에서 동작 되어야 함
  @EventListener
  public void onOrderStatusChanged(OrderStatusChangedEvent event) {
    Long boothId = event.boothId();
    Long orderId = event.orderId();
    OrderStatus previousStatus = event.previousStatus();
    OrderStatus newStatus = event.newStatus();
    Order order = event.order();
    switch (newStatus) {
      case WAITING -> {
        List<WaitingOrderItemResponse> waitingOrderItems =
            orderItemRepository.findWaitingOrderItemsByOrderIds(List.of(orderId));

        eventPublisher.publishEvent(
            orderEventMapper.toWaitingOrderPayload(
                boothId, orderMapper.toWaitingOrderResponseFromDto(order, waitingOrderItems)));
        eventPublisher.publishEvent(
            orderEventMapper.toDismissOrderPayload(boothId, previousStatus, orderId));
        log.debug("[OrderService] 취소 > 대기 이벤트 발행 - 주문 식별자: {}", orderId);
      }
      case COOKING -> {
        List<Long> orderItemIds = orderItemRepository.findOrderItemIdsByOrderIds(List.of(orderId));

        List<CookingOrderItemUnitResponse> cookingOrderItemUnits =
            orderItemIds.isEmpty()
                ? List.of()
                : orderItemUnitRepository.findCookingOrderItemUnitsByOrderItemIds(orderItemIds);

        CookingOrderResponse cookingOrderResponse =
            orderMapper.toCookingOrderResponseFromDto(order, cookingOrderItemUnits);

        eventPublisher.publishEvent(
            orderEventMapper.toCookingOrderPayload(
                boothId, cookingOrderResponse, previousStatus, newStatus));
        eventPublisher.publishEvent(
            orderEventMapper.toDismissOrderPayload(boothId, previousStatus, orderId));
        // 만약 대기 중에서 보낸 거라면, 대기중 탭을 제외한 사람들에게 알림 숫자 2 라면 1로 바뀌게 필요
        log.debug("[OrderService] 조리 중 이벤트 발행 - 주문 식별자: {}", orderId);
      }
      case COMPLETED -> {
        List<CompletedOrderItemResponse> completedOrderItems =
            orderItemRepository.findCompletedOrderItemsByOrderIds(List.of(order.getId()));

        eventPublisher.publishEvent(
            orderEventMapper.toCompletedOrderPayload(
                boothId,
                orderMapper.toCompletedOrderResponseFromDto(order, completedOrderItems),
                previousStatus,
                newStatus));
        eventPublisher.publishEvent(
            orderEventMapper.toDismissOrderPayload(boothId, previousStatus, orderId));
        // 만약 조리 중에서 보낸 거라면, 조리중 탭을 제외한 사람들에게 조리중 탭의 알림 숫자를 2 라면 1로 바뀌게 필요
        log.debug("[OrderService] 조리 > 완료 이벤트 발행 - 주문 식별자: {}", orderId);
      }
    }
  }
}
