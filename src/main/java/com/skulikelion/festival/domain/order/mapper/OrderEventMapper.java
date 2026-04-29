/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.mapper;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.order.dto.event.CanceledOrderPayload;
import com.skulikelion.festival.domain.order.dto.event.CompletedOrderPayload;
import com.skulikelion.festival.domain.order.dto.event.CookingOrderPayload;
import com.skulikelion.festival.domain.order.dto.event.OrderIdempotencyPayload;
import com.skulikelion.festival.domain.order.dto.event.OrderItemUnitStatusPayload;
import com.skulikelion.festival.domain.order.dto.event.WaitingOrderPayload;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderItemUnitStatusResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;

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
      Booth booth, CookingOrderResponse cookingOrderResponse) {
    return CookingOrderPayload.builder()
        .cookingOrderResponse(cookingOrderResponse)
        .booth(booth)
        .build();
  }

  public CompletedOrderPayload toCompletedOrderPayload(
      Booth booth, CompletedOrderResponse completedOrderResponse) {
    return CompletedOrderPayload.builder()
        .completedOrderResponse(completedOrderResponse)
        .booth(booth)
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

  public OrderIdempotencyPayload toOrderIdempotencyPayload(
      String idempotencyKey, OrderResponse orderResponse) {
    return OrderIdempotencyPayload.builder()
        .idempotencyKey(idempotencyKey)
        .orderResponse(orderResponse)
        .build();
  }
}
