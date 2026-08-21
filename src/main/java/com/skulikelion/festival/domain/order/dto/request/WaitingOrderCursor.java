/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.request;

import java.time.LocalDateTime;

public record WaitingOrderCursor(LocalDateTime lastCreatedAt, Long lastOrderId) {}
