/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.mapper;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.order.dto.payload.CanceledOrderPayload;
import com.skulikelion.festival.domain.order.dto.payload.CompletedOrderPayload;
import com.skulikelion.festival.domain.order.dto.payload.CookingOrderPayload;
import com.skulikelion.festival.domain.order.dto.payload.DismissOrderPayload;
import com.skulikelion.festival.domain.order.dto.payload.OrderIdempotencyPayload;
import com.skulikelion.festival.domain.order.dto.payload.OrderItemUnitStatusPayload;
import com.skulikelion.festival.domain.order.dto.payload.WaitingOrderPayload;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderItemUnitStatusResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;

@Component
public class OrderEventMapper {

  public WaitingOrderPayload toWaitingOrderPayload(
      Booth booth, WaitingOrderResponse waitingOrderResponse) {
    return WaitingOrderPayload.builder()
        .waitingOrderResponse(waitingOrderResponse)
        .booth(booth)
        .build();
  }

  public CookingOrderPayload toCookingOrderPayload(
      Booth booth,
      CookingOrderResponse cookingOrderResponse,
      OrderStatus previousStatus,
      OrderStatus currentStatus) {
    return CookingOrderPayload.builder()
        .cookingOrderResponse(cookingOrderResponse)
        .booth(booth)
        .previousStatus(previousStatus)
        .currentStatus(currentStatus)
        .build();
  }

  public CompletedOrderPayload toCompletedOrderPayload(
      Booth booth,
      CompletedOrderResponse completedOrderResponse,
      OrderStatus previousStatus,
      OrderStatus currentStatus) {
    return CompletedOrderPayload.builder()
        .completedOrderResponse(completedOrderResponse)
        .booth(booth)
        .previousStatus(previousStatus)
        .currentStatus(currentStatus)
        .build();
  }

  public CanceledOrderPayload toCanceledOrderPayload(
      Booth booth, CanceledOrderResponse canceledOrderResponse) {
    return CanceledOrderPayload.builder()
        .canceledOrderResponse(canceledOrderResponse)
        .booth(booth)
        .build();
  }

  public OrderItemUnitStatusPayload toCookingOrderItemUnitPayload(
      Booth booth, OrderItemUnitStatusResponse orderItemUnitStatusResponse) {
    return OrderItemUnitStatusPayload.builder()
        .booth(booth)
        .orderItemUnitStatusResponse(orderItemUnitStatusResponse)
        .build();
  }

  public DismissOrderPayload toDismissOrderPayload(
      Booth booth, OrderStatus orderStatus, Long orderId) {
    return DismissOrderPayload.builder()
        .booth(booth)
        .currentOrderStatus(orderStatus)
        .orderId(orderId)
        .build();
  }

  public OrderIdempotencyPayload toOrderIdempotencyPayload(
      String idempotencyKey, OrderResponse orderResponse) {
    return OrderIdempotencyPayload.builder()
        .idempotencyKey(idempotencyKey)
        .orderResponse(orderResponse)
        .build();
  }
}
