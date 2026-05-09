/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "부스 상태")
public enum BoothStatus {
  OPEN("영업중"),
  SOLD_OUT("재료 소진"),
  BREAK_TIME("브레이크 타임"),
  LAST_ORDER_CLOSED("라스트 오더 마감"),
  BOOTH_REASON_CLOSED("부스 사정"),
  CLOSED("영업종료");

  private final String description;
}
