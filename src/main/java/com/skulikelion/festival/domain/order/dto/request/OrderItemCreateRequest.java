/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(title = "OrderItemCreateRequest: 주문 상세 생성 요청 DTO")
public class OrderItemCreateRequest {

  private Long boothMenuId;

  private Integer quantity;

  private Integer menuPrice;

  private Integer totalOrderItemPrice;
}
