/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.enums;

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
}
