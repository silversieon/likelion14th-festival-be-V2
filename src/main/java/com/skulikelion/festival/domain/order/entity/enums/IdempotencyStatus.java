/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.entity.enums;

import lombok.Getter;

@Getter
public enum IdempotencyStatus {
  PROCESSING,
  DONE;
}
