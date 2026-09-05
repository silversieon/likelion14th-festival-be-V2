/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.event.payload;

import com.skulikelion.festival.domain.order.entity.Order;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;

public record OrderStatusChangedEvent(
    Long boothId, Long orderId, OrderStatus previousStatus, OrderStatus newStatus, Order order) {}
