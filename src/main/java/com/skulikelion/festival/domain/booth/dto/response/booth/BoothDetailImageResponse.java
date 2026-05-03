/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.response.booth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "BoothDetailImageResponse: 부스 상세 이미지 응답 DTO")
public class BoothDetailImageResponse {

  @Schema(description = "이미지 URL")
  private String imageUrl;
}
