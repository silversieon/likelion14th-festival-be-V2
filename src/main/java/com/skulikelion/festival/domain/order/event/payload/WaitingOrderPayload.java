/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.payload;

import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;

import lombok.Builder;

@Builder
public record WaitingOrderPayload(
    Long boothId, OrderStatus waitingStatus, WaitingOrderResponse waitingOrderResponse) {

  public static WaitingOrderPayload of(Long boothId, WaitingOrderResponse waitingOrderResponse) {
    return new WaitingOrderPayload(boothId, OrderStatus.WAITING, waitingOrderResponse);
  }
}
