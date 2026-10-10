/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.statistics.repository.projection;

import java.time.LocalDate;

/** 일별 매출 집계 결과 한 행입니다. 주문이 있는 날짜만 나온다. */
public interface DailySalesRow {

  LocalDate getSalesDate();

  Long getTotalSales();

  Long getOrderCount();
}
