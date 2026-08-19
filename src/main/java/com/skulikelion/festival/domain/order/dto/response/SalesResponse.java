/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "SalesResponse: 매출 응답 DTO")
public class SalesResponse {

  @Schema(description = "매출 값", example = "550000")
  private Long sales;

  public SalesResponse(Long sales) {
    this.sales = sales == null ? 0L : sales;
  }
}
