/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.event.payload;

import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;

import lombok.Builder;

@Builder
public record CompletedOrderPayload(
    Long boothId,
    CompletedOrderResponse completedOrderResponse,
    OrderStatus previousStatus,
    OrderStatus currentStatus) {}
