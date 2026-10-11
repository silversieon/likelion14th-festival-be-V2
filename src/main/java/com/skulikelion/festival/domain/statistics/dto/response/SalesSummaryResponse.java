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
@Schema(title = "SalesSummaryResponse: 총 주문 수·총 수익 응답 DTO")
public class SalesSummaryResponse {

  @Schema(description = "완료된 주문 수", example = "4810")
  private Long totalOrders;

  @Schema(description = "완료된 주문의 총 주문 금액", example = "31245000")
  private Long totalSales;
}
