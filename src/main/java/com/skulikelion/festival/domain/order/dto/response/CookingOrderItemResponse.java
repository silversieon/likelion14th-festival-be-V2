/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(title = "CookingOrderItemResponse: 조리 중인 주문 상세 응답 DTO")
public class CookingOrderItemResponse {

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

  @Schema(description = "주문한 메뉴 개별 목록")
  private List<CookingOrderItemUnitResponse> orderItemUnits;

  public CookingOrderItemResponse(
      Long orderItemId,
      Long orderId,
      String menuName,
      Integer quantity,
      Integer totalOrderItemPrice) {
    this.orderItemId = orderItemId;
    this.orderId = orderId;
    this.menuName = menuName;
    this.quantity = quantity;
    this.totalOrderItemPrice = totalOrderItemPrice;
  }

  public void addOrderItemUnits(List<CookingOrderItemUnitResponse> orderItemUnits) {
    this.orderItemUnits = orderItemUnits;
  }
}
