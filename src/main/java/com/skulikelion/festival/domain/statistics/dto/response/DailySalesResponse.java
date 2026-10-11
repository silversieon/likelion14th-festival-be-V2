/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.statistics.dto.response;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "DailySalesResponse: 일별 매출 응답 DTO")
public class DailySalesResponse {

  @Schema(description = "일자", example = "2026-10-10")
  private LocalDate date;

  @Schema(description = "그날 완료된 주문의 총 주문 금액", example = "1520000")
  private Long totalSales;

  @Schema(description = "그날 완료된 주문 수", example = "231")
  private Long orderCount;
}
