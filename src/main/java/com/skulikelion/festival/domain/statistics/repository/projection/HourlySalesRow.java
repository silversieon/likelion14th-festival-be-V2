/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.statistics.repository.projection;

/** 시간대별 매출 집계 결과 한 행입니다. 주문이 있는 11~23시 구간만 나온다. */
public interface HourlySalesRow {

  Integer getSalesHour();

  Long getTotalSales();

  Long getOrderCount();
}
