/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.response.menu;

import com.skulikelion.festival.domain.booth.enums.MenuCategory;
import com.skulikelion.festival.domain.booth.enums.TimeType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "BoothMenuResponse: 부스 메뉴 응답 DTO")
public class BoothMenuResponse {

  @Schema(description = "메뉴 식별자", example = "1")
  private Long menuId;

  @Schema(description = "메뉴명")
  private String name;

  @Schema(description = "가격")
  private Integer price;

  @Schema(description = "운영 시간 타입")
  private TimeType timeType;

  @Schema(description = "품절 여부")
  private Boolean soldOut;

  @Schema(description = "메뉴 설명")
  private String description;

  @Schema(description = "메뉴 카테고리")
  private MenuCategory category;

  @Schema(description = "아이콘 이미지 URL")
  private String iconImageUrl;
}
