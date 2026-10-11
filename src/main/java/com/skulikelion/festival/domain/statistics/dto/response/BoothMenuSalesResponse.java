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
@Schema(title = "BoothMenuSalesResponse: 부스별 인기 메뉴 순위 응답 DTO")
public class BoothMenuSalesResponse {

  @Schema(description = "부스 안에서의 순위", example = "1")
  private Long rank;

  @Schema(description = "부스 식별자", example = "1")
  private Long boothId;

  @Schema(description = "메뉴 식별자", example = "11")
  private Long boothMenuId;

  @Schema(description = "학교명", example = "서경대학교")
  private String universityName;

  @Schema(description = "학과명 (부스명)", example = "소프트웨어학과")
  private String departmentName;

  @Schema(description = "메뉴명", example = "닭꼬치")
  private String menuName;

  @Schema(description = "주문 수 (판매 수량의 합)", example = "612")
  private Long quantity;

  @Schema(description = "총 수익", example = "2448000")
  private Long sales;
}
