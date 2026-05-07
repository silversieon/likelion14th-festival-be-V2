/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.request.menu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.skulikelion.festival.domain.booth.enums.MenuCategory;
import com.skulikelion.festival.domain.booth.enums.TimeType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "BoothMenuRequest: 부스 메뉴 생성/수정 요청 DTO")
public class BoothMenuRequest {

  @NotBlank(message = "한국어 메뉴명을 입력해주세요.")
  @Schema(description = "한국어 메뉴명", example = "해물야끼우동")
  private String nameKo;

  @NotBlank(message = "영어 메뉴명을 입력해주세요.")
  @Schema(description = "영어 메뉴명", example = "Seafood Yaki Udon")
  private String nameEn;

  @NotBlank(message = "중국어 메뉴명을 입력해주세요.")
  @Schema(description = "중국어 메뉴명", example = "海鲜炒乌冬面")
  private String nameZh;

  @NotNull(message = "가격을 입력해주세요.") @Schema(description = "가격", example = "11000")
  private Integer price;

  @NotNull(message = "운영 시간 타입을 입력해주세요.") @Schema(description = "운영 시간 타입", example = "NIGHT")
  private TimeType timeType;

  @Schema(description = "품절 여부", example = "false")
  private Boolean soldOut;

  @Schema(description = "한국어 메뉴 설명")
  private String descriptionKo;

  @Schema(description = "영어 메뉴 설명")
  private String descriptionEn;

  @Schema(description = "중국어 메뉴 설명")
  private String descriptionZh;

  @NotNull(message = "메뉴 카테고리를 입력해주세요.") @Schema(description = "메뉴 카테고리", example = "MAIN")
  private MenuCategory category;
}
