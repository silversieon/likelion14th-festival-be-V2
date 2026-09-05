/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.mapper;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderItemUnitStatusResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;
import com.skulikelion.festival.domain.order.event.payload.CanceledOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.CompletedOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.CookingOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.DismissOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.OrderItemUnitStatusPayload;
import com.skulikelion.festival.domain.order.event.payload.WaitingOrderPayload;

@Component
public class OrderEventMapper {

  public WaitingOrderPayload toWaitingOrderPayload(
      Long boothId, WaitingOrderResponse waitingOrderResponse) {
    return WaitingOrderPayload.of(boothId, waitingOrderResponse);
  }

  public CookingOrderPayload toCookingOrderPayload(
      Long boothId,
      CookingOrderResponse cookingOrderResponse,
      OrderStatus previousStatus,
      OrderStatus currentStatus) {
    return CookingOrderPayload.builder()
        .cookingOrderResponse(cookingOrderResponse)
        .boothId(boothId)
        .previousStatus(previousStatus)
        .currentStatus(currentStatus)
        .build();
  }

  public CompletedOrderPayload toCompletedOrderPayload(
      Long boothId,
      CompletedOrderResponse completedOrderResponse,
      OrderStatus previousStatus,
      OrderStatus currentStatus) {
    return CompletedOrderPayload.builder()
        .completedOrderResponse(completedOrderResponse)
        .boothId(boothId)
        .previousStatus(previousStatus)
        .currentStatus(currentStatus)
        .build();
  }

  public CanceledOrderPayload toCanceledOrderPayload(
      Long boothId, CanceledOrderResponse canceledOrderResponse) {
    return CanceledOrderPayload.builder()
        .canceledOrderResponse(canceledOrderResponse)
        .boothId(boothId)
        .build();
  }

  public OrderItemUnitStatusPayload toCookingOrderItemUnitPayload(
      Long boothId, OrderItemUnitStatusResponse orderItemUnitStatusResponse) {
    return OrderItemUnitStatusPayload.builder()
        .boothId(boothId)
        .orderItemUnitStatusResponse(orderItemUnitStatusResponse)
        .build();
  }

  public DismissOrderPayload toDismissOrderPayload(
      Long boothId, OrderStatus orderStatus, Long orderId) {
    return DismissOrderPayload.builder()
        .boothId(boothId)
        .currentOrderStatus(orderStatus)
        .orderId(orderId)
        .build();
  }
}
