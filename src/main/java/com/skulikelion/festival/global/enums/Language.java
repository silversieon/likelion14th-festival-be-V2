/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.enums;

import com.skulikelion.festival.domain.booth.entity.BoothMenu;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "언어 타입")
public enum Language {
  KO("한국어"),
  EN("영어"),
  ZH("중국어");

  private final String description;

  public String getMenuName(BoothMenu menu) {
    return switch (this) {
      case KO -> menu.getNameKo();
      case EN -> menu.getNameEn();
      case ZH -> menu.getNameZh();
    };
  }
}
