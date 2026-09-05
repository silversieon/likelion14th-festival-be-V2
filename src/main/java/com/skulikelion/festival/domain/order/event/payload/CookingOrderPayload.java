/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.payload;

import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;

import lombok.Builder;

@Builder
public record CookingOrderPayload(
    Long boothId,
    CookingOrderResponse cookingOrderResponse,
    OrderStatus previousStatus,
    OrderStatus currentStatus) {}
