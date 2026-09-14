/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.university.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 대학이 속한 광역자치단체(시·도)입니다.
 *
 * <p>전국 17개 광역시·도로 고정되어 있어 enum으로 둔다. 저장은 ERD 관례에 따라 MySQL {@code ENUM}이 아니라 {@code VARCHAR(50)} +
 * {@code @Enumerated(STRING)}으로 한다.
 *
 * @since 2026.09.14
 * @see com.skulikelion.festival.domain.university.entity.University
 */
@Getter
@RequiredArgsConstructor
@Schema(description = "대학이 속한 시도")
public enum Region {
  SEOUL("서울특별시"),
  BUSAN("부산광역시"),
  DAEGU("대구광역시"),
  INCHEON("인천광역시"),
  GWANGJU("광주광역시"),
  DAEJEON("대전광역시"),
  ULSAN("울산광역시"),
  SEJONG("세종특별자치시"),
  GYEONGGI("경기도"),
  GANGWON("강원특별자치도"),
  CHUNGBUK("충청북도"),
  CHUNGNAM("충청남도"),
  JEONBUK("전북특별자치도"),
  JEONNAM("전라남도"),
  GYEONGBUK("경상북도"),
  GYEONGNAM("경상남도"),
  JEJU("제주특별자치도");

  private final String description;
}
