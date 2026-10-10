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
@Schema(title = "BoothSalesResponse: 부스별 매출 응답 DTO")
public class BoothSalesResponse {

  @Schema(description = "전체 부스 기준 매출 순위", example = "1")
  private Long rank;

  @Schema(description = "부스 식별자", example = "1")
  private Long boothId;

  @Schema(description = "학교명", example = "서경대학교")
  private String universityName;

  @Schema(description = "학과명 (부스명)", example = "소프트웨어학과")
  private String departmentName;

  @Schema(description = "총 매출", example = "8603000")
  private Long totalSales;

  @Schema(description = "평균 주문 금액 (반올림)", example = "8436")
  private Long averageOrderAmount;

  @Schema(description = "총 주문 건수", example = "1020")
  private Long orderCount;

  @Schema(description = "총 판매 메뉴 수", example = "1931")
  private Long totalQuantity;
}
