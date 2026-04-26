/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.mapper;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.order.dto.event.CanceledOrderPayload;
import com.skulikelion.festival.domain.order.dto.event.CompletedOrderPayload;
import com.skulikelion.festival.domain.order.dto.event.CookingOrderItemUnitPayload;
import com.skulikelion.festival.domain.order.dto.event.CookingOrderPayload;
import com.skulikelion.festival.domain.order.dto.event.WaitingOrderPayload;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderItemUnitResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
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

  public CookingOrderItemUnitPayload toCookingOrderItemUnitPayload(
      Booth booth, CookingOrderItemUnitResponse cookingOrderItemUnitResponse) {
    return CookingOrderItemUnitPayload.builder()
        .booth(booth)
        .cookingOrderItemUnitResponse(cookingOrderItemUnitResponse)
        .build();
  }
}
