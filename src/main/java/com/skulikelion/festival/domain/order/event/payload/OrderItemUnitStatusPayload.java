/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.event.payload;

import com.skulikelion.festival.domain.order.dto.response.OrderItemUnitStatusResponse;

import lombok.Builder;

@Builder
public record OrderItemUnitStatusPayload(
    Long boothId, OrderItemUnitStatusResponse orderItemUnitStatusResponse) {}
