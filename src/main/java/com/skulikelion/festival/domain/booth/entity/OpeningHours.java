/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.entity;

import io.swagger.v3.oas.annotations.media.Schema;

public enum OpeningHours {
  @Schema(description = "낮")
  DAY,
  @Schema(description = "밤")
  NIGHT,
  @Schema(description = "종일")
  FULL
}
