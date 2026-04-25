/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "메뉴 카테고리")
public enum MenuCategory {
  MAIN("메인"),
  SIDE("사이드"),
  DRINK("음료");

  private final String description;
}
