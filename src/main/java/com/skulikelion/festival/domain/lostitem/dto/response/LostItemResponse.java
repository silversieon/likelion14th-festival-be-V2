/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LostItemResponse {

  @Schema(description = "분실물 ID", example = "1")
  private final Long id;

  @Schema(description = "분실물 이름", example = "에어팟")
  private final String name;

  @Schema(description = "이미지 URL", example = "https://example.com/image.jpg")
  private final String imageUrl;

  @Schema(description = "습득 장소", example = "북악관")
  private final String foundPlace;

  @Schema(description = "습득 날짜", example = "2025-04-21")
  private final LocalDate foundDate;

  @Schema(description = "수령 여부", example = "true")
  private final boolean isReturned;

  @Schema(description = "등록 시각")
  private final LocalDateTime createdAt;
}
