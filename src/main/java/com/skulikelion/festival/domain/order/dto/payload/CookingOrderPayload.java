/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.payload;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;

import lombok.Builder;

@Builder
public record CookingOrderPayload(
    Booth booth,
    CookingOrderResponse cookingOrderResponse,
    OrderStatus previousStatus,
    OrderStatus currentStatus) {}
