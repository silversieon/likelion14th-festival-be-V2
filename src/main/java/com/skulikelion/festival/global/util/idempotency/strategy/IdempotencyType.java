/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency;

public enum IdempotencyType {
  DB,
  REDIS,
  WRITETHROUGH,
  FALLBACK
}
