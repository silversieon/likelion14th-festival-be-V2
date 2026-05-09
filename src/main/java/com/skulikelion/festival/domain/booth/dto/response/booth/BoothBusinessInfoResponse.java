/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.response.booth;

import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "BoothBusinessInfoResponse: 부스 영업 정보 응답 DTO")
public class BoothBusinessInfoResponse {

  @Schema(description = "학과명", example = "소프트웨어학과")
  private String departmentName;

  @Schema(description = "부스 영업 중 여부", example = "true")
  private boolean isActive;

  @Schema(description = "낮 오픈 시간", example = "12:00")
  private LocalTime dayOpenTime;

  @Schema(description = "밤 오픈 시간", example = "18:00")
  private LocalTime nightOpenTime;

  @Schema(description = "마감 시간", example = "22:00")
  private LocalTime closeTime;

  @Schema(description = "매출", example = "550000")
  private Long sales;
}
