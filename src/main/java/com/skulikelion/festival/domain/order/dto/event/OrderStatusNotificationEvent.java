/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.event;

import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;

public record OrderStatusNotificationEvent(OrderStatus orderStatus) {}
