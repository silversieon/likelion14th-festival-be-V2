/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.request;

import jakarta.validation.constraints.NotBlank;

import com.skulikelion.festival.domain.booth.entity.OpeningHours;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "BoothMenuRequest DTO", description = "부스 메뉴 생성 및 수정을 위한 데이터 전송")
public class BoothMenuRequest {

  @NotBlank(message = "부스 이름 항목은 필수입니다.")
  @Schema(description = "부스 이름", example = "디자인학부")
  private String name;

  @Schema(description = "수정할 메뉴의 한국어 이름", example = "감자튀김")
  private String boothMenu;

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

  @Schema(
      description = "영업 시간",
      example = "DAY",
      allowableValues = {"DAY", "NIGHT", "FULL"})
  private OpeningHours menuTimeType;
}
