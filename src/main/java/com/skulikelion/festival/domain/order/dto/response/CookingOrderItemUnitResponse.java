/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(title = "CookingOrderItemUnitResponse: 조리 중인 주문 상세 개별 응답 DTO")
public class CookingOrderItemUnitResponse {

  @Schema(description = "주문 상세 개별 식별자", example = "1")
  private Long orderItemUnitId;

  @Schema(description = "주문 상세 식별자", example = "1")
  private Long orderItemId;

  @JsonProperty("isServed")
  @Schema(description = "완료(또는 서빙) 여부", example = "true")
  private boolean served;
}
