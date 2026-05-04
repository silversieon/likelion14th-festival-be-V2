/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.response.booth;

import java.util.List;

import com.skulikelion.festival.domain.booth.dto.response.menu.BoothMenuSummaryGroupResponse;
import com.skulikelion.festival.domain.booth.enums.BoothLocation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "BoothResponse: 부스 단건 응답 DTO")
public class BoothResponse {

  @Schema(description = "부스 식별자", example = "1")
  private Long boothId;

  @Schema(description = "썸네일 이미지 URL")
  private String thumbnailUrl;

  @Schema(description = "영업중 여부")
  private boolean open;

  @Schema(description = "주문 버튼 활성화 여부")
  private boolean orderAvailable;

  @Schema(description = "부스 위치")
  private BoothLocation location;

  @Schema(description = "부스 위치 설명")
  private String locationDescription;

  @Schema(description = "부스 번호 목록")
  private List<Integer> boothNumbers;

  @Schema(description = "학과명")
  private String departmentName;

  @Schema(description = "부스명")
  private String boothName;

  @Schema(description = "부스 설명")
  private String description;

  @Schema(description = "상세 이미지 목록")
  private List<BoothDetailImageResponse> detailImages;

  @Schema(description = "낮/밤 메뉴 목록")
  private BoothMenuSummaryGroupResponse menus;
}
