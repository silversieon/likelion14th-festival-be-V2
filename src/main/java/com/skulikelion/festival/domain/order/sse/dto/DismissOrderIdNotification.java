/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.dto;

public record DismissOrderIdNotification(Long orderId) {

  public static DismissOrderIdNotification of(Long orderId) {
    return new DismissOrderIdNotification(orderId);
  }
}
