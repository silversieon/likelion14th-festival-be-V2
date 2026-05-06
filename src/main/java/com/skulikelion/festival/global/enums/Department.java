/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.enums;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.skulikelion.festival.domain.auth.exception.AuthErrorCode;
import com.skulikelion.festival.global.exception.CustomException;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "학과")
public enum Department {

  // 사회과학대
  SOCIAL("여정 사회과학대"),
  BUSINESS("경영학부"),
  CHILD_YOUTH("아동청소년학과"),
  PUBLIC("공공인재학부"),
  MILITARY("군사학과"),

  // 인문대
  CHINESE("중어전공"),
  JAPANESE("일어전공"),

  // 이공대
  ENGINEERING("연화무적이공대"),
  FINANCE("금융정보공학과"),
  NANO_BIO("나노화학생명공학과"),
  ARCHITECTURE("토목건축공학과"),
  SOFTWARE("소프트웨어학과"),
  ELECTRONIC_COMPUTER("전자컴퓨터공학과"),
  URBAN("도시공학과"),
  LOGISTICS("물류시스템공학과"),

  // 예술대
  ARTS("이음 통합예술대"),
  FILM("영화영상학과"),
  BEAUTY("미용예술학부"),
  PRACTICAL_MUSIC("실용음악학부"),
  DESIGN_VDLD("디자인학부(비디, 라디)"),
  DESIGN_VOID("디자인학부(VOID(보이드)/sesi)"),
  AD_PR("광고홍보영상학과"),

  // 융합대
  CONVERGENCE("모아 융합대"),
  SPORTS_TECH("스포츠앤테크놀로지학과"),

  // 미융대
  FUTURE_1("미래융합대학1"),
  FUTURE_2("미래융합대학2"),

  // 자치기구
  CLUB("총동아리연합회"),
  PRESS("신문사"),

  // 총학생회
  STUDENT_COUNCIL("총학생회"),

  FESTIVAL_PLANNING("축제기획단"),
  STUDENT_OFFICE("학생처"),

  // 멋사 운영진
  SKULIKELION("멋사 운영진");

  private final String description;

  @JsonCreator
  public static Department from(String value) {
    return Arrays.stream(Department.values())
        .filter(d -> d.name().equals(value))
        .findFirst()
        .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_DEPARTMENT));
  }
}
