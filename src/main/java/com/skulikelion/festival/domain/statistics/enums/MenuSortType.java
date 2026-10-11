/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.statistics.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 메뉴 TOP 10의 정렬 기준입니다. (LLD-0005 2.3 R13) */
@Getter
@RequiredArgsConstructor
@Schema(description = "메뉴 순위 정렬 기준")
public enum MenuSortType {
  QUANTITY("판매순"),
  SALES("수익순");

  private final String description;
}
