/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency.strategy.db.converter;

import java.util.UUID;

public class DbKeyConverter {

  public static UUID convertKeyToDbKey(String idempotencyKey) {
    return UUID.fromString(idempotencyKey);
  }
}
