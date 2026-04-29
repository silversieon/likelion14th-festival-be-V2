/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.entity.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "주문 취소 사유")
public enum OrderCancelReason {
  CUSTOMER_REQUEST("고객 요청"),
  ORDER_MISTAKE("주문 실수"),
  OUT_OF_STOCK("재료 소진"),
  ETC("기타");

  private final String description;
}
