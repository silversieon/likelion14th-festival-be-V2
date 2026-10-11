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
@Schema(title = "MenuSalesResponse: 메뉴 TOP 10 응답 DTO")
public class MenuSalesResponse {

  @Schema(description = "전체 메뉴 기준 순위", example = "1")
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

  @Schema(description = "판매량", example = "612")
  private Long quantity;

  @Schema(description = "총 수익", example = "2448000")
  private Long sales;
}
