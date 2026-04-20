/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.response;

import java.util.List;

import com.skulikelion.festival.domain.booth.entity.OpeningHours;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "BoothResponse DTO", description = "부스 정보 응답 반환")
public class BoothResponse {

  @Schema(description = "부스 식별자", example = "ㅂ")
  private Long id;

  @Schema(description = "부스 이름", example = "디자인학부")
  private String name;

  @Schema(description = "비밀번호", example = "password123!")
  private String password;

  @Schema(description = "대기 팀 수", example = "20")
  private Integer waitingTeam;

  @Schema(description = "영업 시간", example = "DAY")
  private OpeningHours openingHours;

  @Schema(description = "부스 썸네일 사진 url")
  private String boothThumbnailUrl;

  @Schema(description = "이미지 URL 리스트")
  private List<String> imageUrls;
}
