/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.request;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "BoothMultipartBody: 부스 multipart 요청 DTO")
public class BoothMultipartBody {

  @Schema(description = "부스 생성/수정 요청")
  private BoothRequest request;

  @Schema(description = "부스 썸네일 이미지")
  private MultipartFile thumbnail;

  @Schema(description = "부스 상세 이미지 목록, 최대 3개")
  private List<MultipartFile> detailImages;
}
