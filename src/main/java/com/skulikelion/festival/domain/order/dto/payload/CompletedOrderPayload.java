/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.payload;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;

import lombok.Builder;

@Builder
public record CompletedOrderPayload(
    Booth booth,
    CompletedOrderResponse completedOrderResponse,
    OrderStatus previousStatus,
    OrderStatus currentStatus) {}
