/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.event;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderItemUnitResponse;

import lombok.Builder;

@Builder
public record CookingOrderItemUnitPayload(
    Booth booth, CookingOrderItemUnitResponse cookingOrderItemUnitResponse) {}
