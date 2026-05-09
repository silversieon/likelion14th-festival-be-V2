/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.response.booth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "BoothThumbnailResponse: 부스 썸네일 수정 응답 DTO")
public class BoothThumbnailResponse {

  @Schema(description = "부스 식별자", example = "1")
  private Long boothId;

  @Schema(description = "수정된 썸네일 이미지 URL")
  private String thumbnailUrl;
}
