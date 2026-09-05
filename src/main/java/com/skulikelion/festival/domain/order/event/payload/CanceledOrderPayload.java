/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.payload;

import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;

import lombok.Builder;

@Builder
public record CanceledOrderPayload(Long boothId, CanceledOrderResponse canceledOrderResponse) {}
