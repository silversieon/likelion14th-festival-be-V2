/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.statistics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "HourlySalesResponse: 시간대별 매출 응답 DTO")
public class HourlySalesResponse {

  @Schema(description = "시각 (11 ~ 23)", example = "18")
  private Integer hour;

  @Schema(description = "해당 시각 구간의 총 주문 금액", example = "120000")
  private Long totalSales;

  @Schema(description = "해당 시각 구간의 주문 수", example = "18")
  private Long orderCount;
}
