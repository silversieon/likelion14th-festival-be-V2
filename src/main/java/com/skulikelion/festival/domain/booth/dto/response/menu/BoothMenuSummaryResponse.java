/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.response.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "BoothMenuSummaryResponse: 부스 메뉴 요약 응답 DTO")
public class BoothMenuSummaryResponse {

  @Schema(description = "메뉴명")
  private String name;

  @Schema(description = "가격")
  private Integer price;
}
