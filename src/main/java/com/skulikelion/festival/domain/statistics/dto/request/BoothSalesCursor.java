/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.statistics.dto.request;

/** 부스별 매출 검색 커서입니다. 순위({@code ROW_NUMBER})가 유일하므로 마지막 순위 하나로 다음 페이지를 찾는다. (LLD-0007 R5) */
public record BoothSalesCursor(Long lastRank) {}
