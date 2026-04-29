/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(title = "OrderItemCreateRequest: 주문 상세 생성 요청 DTO")
public class OrderItemCreateRequest {

  @NotNull @Positive @Schema(description = "부스 메뉴 식별자", example = "1")
  private Long boothMenuId;

  @NotNull @Positive @Schema(description = "메뉴 개수", example = "2")
  private Integer quantity;

  @NotNull @PositiveOrZero
  @Schema(description = "메뉴 가격", example = "12000")
  private Integer menuPrice;

  @NotNull @PositiveOrZero
  @Schema(description = "주문 상세 총 가격", example = "24000")
  private Integer totalOrderItemPrice;
}
