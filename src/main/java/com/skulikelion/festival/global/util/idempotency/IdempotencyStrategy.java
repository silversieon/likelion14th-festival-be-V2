/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency;

public enum IdempotencyStrategy {
  DB,
  REDIS,
  WRITETHROUGH,
  FALLBACK
}
