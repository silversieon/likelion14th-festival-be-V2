/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency.strategy.db.entity;

import lombok.Getter;

@Getter
public enum IdempotencyStatus {
  PROCESSING,
  DONE;
}
