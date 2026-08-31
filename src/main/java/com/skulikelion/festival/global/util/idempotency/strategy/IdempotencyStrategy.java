/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency;

import java.util.function.Supplier;

public interface IdempotencyStrategy {

  <T> T executeIdempotent(String idempotencyKey, Supplier<T> processor, Class<T> responseType);

  IdempotencyType getType();
}
