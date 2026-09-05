/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.payload;

import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;

import lombok.Builder;

@Builder
public record DismissOrderPayload(Long boothId, OrderStatus currentOrderStatus, Long orderId) {}
