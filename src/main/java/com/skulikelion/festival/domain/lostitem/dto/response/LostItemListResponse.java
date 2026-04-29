/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.dto.response;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "LostItemListResponse", description = "분실물 목록 조회 DTO")
public class LostItemListResponse {

  @Schema(description = "분실물 ID", example = "1")
  private Long id;

  @Schema(description = "분실물 이름", example = "에어팟")
  private String name;

  @Schema(description = "대표 이미지 URL, 첫 번째로 등록된 이미지", example = "https://example.com/image.jpg")
  private String imageUrl;

  @Schema(description = "습득 장소", example = "북악관")
  private String foundPlace;

  @Schema(description = "습득 날짜", example = "2026-05-13")
  private LocalDate foundDate;

  @Schema(description = "습득 요일", example = "WEDNESDAY")
  private String dayOfWeek;
}
