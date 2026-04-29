/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.event;

import com.skulikelion.festival.domain.order.dto.response.OrderResponse;

import lombok.Builder;

@Builder
public record OrderIdempotencyPayload(String idempotencyKey, OrderResponse orderResponse) {}
