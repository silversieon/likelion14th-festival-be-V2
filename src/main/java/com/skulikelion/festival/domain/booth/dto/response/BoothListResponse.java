/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "BoothListResponse: 부스 목록 응답 DTO")
public class BoothListResponse {

  @Schema(description = "부스 식별자", example = "1")
  private Long boothId;

  @Schema(description = "썸네일 이미지 URL")
  private String thumbnailUrl;

  @Schema(description = "부스 상세 위치")
  private String locationDetail;

  @Schema(description = "학과명")
  private String departmentName;

  @Schema(description = "부스명")
  private String boothName;
}
