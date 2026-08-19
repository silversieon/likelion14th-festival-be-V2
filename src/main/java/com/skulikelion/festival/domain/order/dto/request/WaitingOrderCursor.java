/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.request;

import java.time.LocalDateTime;
import java.util.Optional;

public record WaitingOrderCursor(LocalDateTime lastCreatedAt, Long lastOrderId) {
}
