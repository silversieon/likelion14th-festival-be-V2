/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.skulikelion.festival.domain.booth.enums.BoothLocation;
import com.skulikelion.festival.global.enums.Department;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "BoothRequest: 부스 생성/수정 요청 DTO")
public class BoothRequest {

  @NotNull(message = "학과를 입력해주세요.") @Schema(description = "학과", example = "SOFTWARE")
  private Department department;

  @Schema(description = "부스 영업 시작 시각(HH:mm)", example = "18:00")
  private String openTime;

  @Schema(description = "주문 가능 시작 시각(HH:mm)", example = "18:30")
  private String orderOpenTime;

  @Schema(description = "부스 영업 종료 시각(HH:mm)", example = "02:00")
  private String closeTime;

  @Schema(description = "부스 위치", example = "YUDAM")
  private BoothLocation location;

  @Schema(description = "부스 상세 위치", example = "유담관 앞")
  private String locationDetail;

  @Schema(description = "예금주", example = "홍길동")
  private String accountName;

  @Schema(description = "계좌번호", example = "123-456-789")
  private String accountNumber;

  @Schema(description = "은행명", example = "국민은행")
  private String bankName;

  @Valid
  @NotEmpty(message = "부스 번역 정보를 입력해주세요.")
  @Schema(description = "부스 번역 정보")
  private List<BoothTranslationRequest> translations;
}
