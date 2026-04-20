/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.minio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MinioResponseDTO {
  @Builder
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(title = "이미지 URL 변환 DTO")
  public static class ImgUrlDTO {
    @Schema(
        description = "업로드된 이미지 접근 URL",
        example =
            "http://2025skufestival.site:9000/skufestival/lost-item/123e4567-e89b-12d3-a456-426614174000.jpg")
    private String imageUrl;
  }
}
