/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.dto.response;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "LostItemResponse", description = "분실물 등록 응답 DTO")
public class LostItemResponse {

  @Schema(description = "분실물 ID", example = "1")
  private final Long id;

  @Schema(description = "분실물 이름", example = "에어팟")
  private final String name;

  @Schema(
      description = "분실물 이미지 URL 리스트, 등록 순서대로 제공",
      example = "[\"https://example.com/image1.jpg\", \"https://example.com/image2.jpg\"]")
  private final List<String> imageUrls;

  @Schema(description = "습득 장소", example = "북악관")
  private final String foundPlace;

  @Schema(description = "습득 날짜", example = "2026-05-13")
  private final LocalDate foundDate;

  @Schema(description = "습득 요일", example = "WEDNESDAY")
  private final String dayOfWeek;

  @Schema(description = "수령 여부", example = "false")
  private final boolean isReturned;
}
