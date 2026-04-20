/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "BoothMenuResponse DTO", description = "메뉴 수정에서 사용하는 메뉴 응답 DTO")
public class BoothMenuResponse {

  @Schema(description = "한국어 메뉴", example = "감자튀김")
  private String menuKo;

  @Schema(description = "영어 메뉴", example = "French fries")
  private String menuEn;

  @Schema(description = "중국어 메뉴", example = "薯条")
  private String menuCh;

  @Schema(description = "일본어 메뉴", example = "フライドポテト")
  private String menuJp;

  @Schema(description = "메뉴 가격", example = "5000")
  private Integer menuPrice;
}
