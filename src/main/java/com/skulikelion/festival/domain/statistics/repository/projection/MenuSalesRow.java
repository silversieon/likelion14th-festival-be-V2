/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.statistics.repository.projection;

/** 메뉴별 판매 집계 결과 한 행입니다. 순위의 기준(부스 안 / 전체)은 쿼리마다 다르다. */
public interface MenuSalesRow {

  Long getMenuRank();

  Long getBoothId();

  Long getBoothMenuId();

  String getUniversityName();

  String getDepartmentName();

  String getMenuName();

  Long getQuantity();

  Long getSales();
}
