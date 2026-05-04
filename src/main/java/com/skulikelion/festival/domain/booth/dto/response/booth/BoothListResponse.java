/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.response.booth;

import java.util.List;

import com.skulikelion.festival.domain.booth.enums.BoothLocation;

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

  @Schema(description = "부스 위치")
  private BoothLocation location;

  @Schema(description = "부스 위치 설명")
  private String locationDescription;

  @Schema(description = "부스 번호 목록")
  private List<Integer> boothNumbers;

  @Schema(description = "학과명")
  private String departmentName;

  public BoothListResponse(
      Long boothId,
      String thumbnailUrl,
      BoothLocation location,
      List<Integer> boothNumbers,
      String departmentName) {
    this.boothId = boothId;
    this.thumbnailUrl = thumbnailUrl;
    this.location = location;
    this.locationDescription = location == null ? null : location.getDescription();
    this.boothNumbers = boothNumbers;
    this.departmentName = departmentName;
  }
}
