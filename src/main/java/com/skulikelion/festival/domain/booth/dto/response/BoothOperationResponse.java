/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

import com.skulikelion.festival.domain.booth.enums.TimeType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "BoothOperationResponse: 부스 운영 정보 응답 DTO")
public class BoothOperationResponse {

  @Schema(description = "운영 날짜", example = "2026-05-07")
  private LocalDate operationDate;

  @Schema(description = "운영 시간 타입", example = "ALL")
  private TimeType timeType;

  @Schema(description = "낮 시작 시간", example = "13:00")
  private LocalTime dayOpenTime;

  @Schema(description = "밤 시작 시간", example = "18:00")
  private LocalTime nightOpenTime;

  @Schema(description = "마감 시간", example = "00:00")
  private LocalTime closeTime;
}
