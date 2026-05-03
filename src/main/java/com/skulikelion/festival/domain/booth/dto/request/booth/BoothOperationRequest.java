/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.request.booth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.skulikelion.festival.domain.booth.enums.TimeType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "BoothOperationRequest: 부스 운영 정보 요청 DTO")
public class BoothOperationRequest {

  @NotBlank(message = "운영 날짜를 입력해주세요.")
  @Schema(description = "운영 날짜(yyyy-MM-dd)", example = "2026-05-07")
  private String operationDate;

  @NotNull(message = "운영 시간 타입을 입력해주세요.") @Schema(description = "운영 시간 타입", example = "ALL")
  private TimeType timeType;

  @Schema(description = "낮 시작 시간(HH:mm), DAY/ALL 필수", example = "13:00")
  private String dayOpenTime;

  @Schema(description = "밤 시작 시간(HH:mm), NIGHT/ALL 필수", example = "18:00")
  private String nightOpenTime;

  @NotBlank(message = "마감 시간을 입력해주세요.")
  @Schema(description = "마감 시간(HH:mm)", example = "00:00")
  private String closeTime;
}
