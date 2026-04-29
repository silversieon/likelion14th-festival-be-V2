/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.skulikelion.festival.global.enums.Language;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "BoothTranslationRequest: 부스 번역 요청 DTO")
public class BoothTranslationRequest {

  @NotNull(message = "언어를 입력해주세요.") @Schema(description = "언어", example = "KO")
  private Language language;

  @NotBlank(message = "학과명을 입력해주세요.")
  @Schema(description = "번역된 학과명", example = "소프트웨어학과")
  private String departmentName;

  @Schema(description = "번역된 부스명", example = "소프트웨어학과 부스")
  private String boothName;

  @Size(max = 150, message = "부스 설명은 150자 이하로 입력해주세요.")
  @Schema(description = "부스 설명", example = "소프트웨어학과 부스입니다.", maxLength = 150)
  private String description;
}
