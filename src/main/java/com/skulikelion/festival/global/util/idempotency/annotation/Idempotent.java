/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.skulikelion.festival.global.util.idempotency.IdempotencyStrategy;

/** */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

  String idempotencyKey();

  IdempotencyStrategy strategy() default IdempotencyStrategy.DB;
}
