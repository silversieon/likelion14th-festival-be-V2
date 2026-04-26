/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(title = "OrderItemUnitUpdateRequest: 주문 상세 개별 수정 요청 DTO")
public class OrderItemUnitUpdateRequest {

  @JsonProperty("isServed")
  @Schema(description = "완료(또는 서빙) 여부", example = "true")
  private boolean served;
}
