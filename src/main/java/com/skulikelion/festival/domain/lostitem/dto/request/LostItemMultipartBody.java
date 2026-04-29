/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.dto.request;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "LostItemMultipartBody: 분실물 multipart 요청 DTO")
public class LostItemMultipartBody {

  @Schema(
      description = "분실물 등록 요청",
      implementation = LostItemRequest.class,
      requiredMode = Schema.RequiredMode.REQUIRED)
  private LostItemRequest request;

  @Schema(description = "분실물 이미지 목록, 최소 1장 최대 4장", requiredMode = Schema.RequiredMode.REQUIRED)
  private List<MultipartFile> images;
}
