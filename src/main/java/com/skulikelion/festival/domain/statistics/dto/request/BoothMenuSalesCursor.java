/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.statistics.dto.request;

/**
 * 부스별 인기 메뉴 순위 커서입니다. 정렬 키 {@code (학교명, 학과명, 부스 id, 부스 안 순위)} 그대로다. 학교명은 지역이 다르면 같을 수 있어 부스 id와 순위가
 * tie-breaker다. (LLD-0007 R6)
 */
public record BoothMenuSalesCursor(
    String lastUniversityName, String lastDepartmentName, Long lastBoothId, Long lastMenuRank) {}
