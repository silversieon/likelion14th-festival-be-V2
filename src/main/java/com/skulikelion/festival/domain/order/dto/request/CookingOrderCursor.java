package com.skulikelion.festival.domain.order.dto.request;

import java.time.LocalDateTime;

public record CookingOrderCursor(LocalDateTime lastModifiedAt,
                                 Long lastOrderId) {
}
