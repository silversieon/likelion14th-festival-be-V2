/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.response.menu;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "BoothMenuSummaryGroupResponse: 부스 메뉴 요약 그룹 응답 DTO")
public class BoothMenuSummaryGroupResponse {

  @Schema(description = "낮 메뉴 목록")
  private List<BoothMenuSummaryResponse> day;

  @Schema(description = "밤 메뉴 목록")
  private List<BoothMenuSummaryResponse> night;
}
