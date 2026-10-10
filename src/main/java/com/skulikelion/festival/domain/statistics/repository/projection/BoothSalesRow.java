/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.statistics.repository.projection;

/** 부스별 매출 집계 결과 한 행입니다. 순위는 전체 부스 기준이다. */
public interface BoothSalesRow {

  Long getSalesRank();

  Long getBoothId();

  String getUniversityName();

  String getDepartmentName();

  Long getTotalSales();

  Long getOrderCount();

  Long getTotalQuantity();
}
