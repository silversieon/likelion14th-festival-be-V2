/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "부스 위치")
public enum BoothLocation {
  DAEIL("대일관"),
  EUNJU_1("은주1관"),
  EUNJU_2("은주2관"),
  CHEONGUN("청운관"),
  HYEIN("혜인관");

  private final String description;
}
