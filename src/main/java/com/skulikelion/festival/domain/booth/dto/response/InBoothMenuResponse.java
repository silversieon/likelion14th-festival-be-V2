/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.response;

import com.skulikelion.festival.domain.booth.entity.BoothMenu;
import com.skulikelion.festival.domain.booth.entity.OpeningHours;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "InBoothMenuResponse DTO", description = "BoothDetailInfoResponse에서 사용하는 메뉴 응답 DTO")
public class InBoothMenuResponse {

  @Schema(description = "선택한 언어의 메뉴 (en, ch, jp)", example = "薯条")
  private String menu;

  @Schema(description = "한국어 메뉴", example = "감자튀김")
  private String menuKR;

  @Schema(description = "메뉴 가격", example = "5000")
  private Integer menuPrice;

  @Schema(description = "메뉴 판매 시간", example = "DAY")
  private OpeningHours menuOpeningHours;

  public InBoothMenuResponse(BoothMenu boothMenu, String lang) {
    this.menuPrice = boothMenu.getMenuPrice();

    switch (lang.toLowerCase()) {
      case "en" -> this.menu = boothMenu.getMenuEn();
      case "ch" -> this.menu = boothMenu.getMenuCh();
      case "jp" -> this.menu = boothMenu.getMenuJp();
      case "ko" -> this.menu = boothMenu.getMenuKo();
    }

    switch (lang.toLowerCase()) {
      case "en", "ch", "jp" -> this.menuKR = boothMenu.getMenuKo();
      case "ko" -> this.menuKR = null;
    }

    this.menuOpeningHours = boothMenu.getMenuTimeType();
  }
}
