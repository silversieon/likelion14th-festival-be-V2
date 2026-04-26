/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderCancelReason {
  CUSTOMER_REQUEST("고객 요청"),
  ORDER_MISTAKE("주문 실수"),
  OUT_OF_STOCK("재료 소진"),
  ETC("기타");

  private final String description;
}
