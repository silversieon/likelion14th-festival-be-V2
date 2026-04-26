/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(title = "WaitingOrderItemResponse: 대기 중인 주문 상세 응답 DTO")
public class WaitingOrderItemResponse {

  @Schema(description = "주문 상세 식별자", example = "1")
  private Long orderItemId;

  @Schema(description = "주문 식별자", example = "1")
  private Long orderId;

  @Schema(description = "메뉴명", example = "해물야끼우동")
  private String menuName;

  @Schema(description = "주문 상세 속 메뉴의 개수", example = "2")
  private Integer quantity;

  @Schema(description = "주문한 메뉴 상세의 총 가격", example = "22000")
  private Integer totalOrderItemPrice;
}
