/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.event;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;

import lombok.Builder;

@Builder
public record CompletedOrderPayload(Booth booth, CompletedOrderResponse completedOrderResponse) {}
