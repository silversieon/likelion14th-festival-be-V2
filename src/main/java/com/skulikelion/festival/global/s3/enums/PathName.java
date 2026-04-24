/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.s3.enums;

import io.swagger.v3.oas.annotations.media.Schema;

public enum PathName {
  @Schema(description = "부스 대표 썸네일 이미지")
  BOOTH_THUMBNAIL,
  @Schema(description = "부스 상세 이미지")
  BOOTH_DETAIL,
  @Schema(description = "분실물 이미지")
  LOST_ITEM,
  @Schema(description = "공통 메뉴 아이콘")
  COMMON_ICON;
}
