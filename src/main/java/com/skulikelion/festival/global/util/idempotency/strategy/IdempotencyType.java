/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency.strategy;

public enum IdempotencyType {
  DB,
  REDIS,
  WRITETHROUGH,
  FALLBACK
}
