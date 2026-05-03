/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.request.menu;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "BoothMenuMultipartBody: 부스 메뉴 multipart 요청 DTO")
public class BoothMenuMultipartBody {

  @Schema(description = "부스 메뉴 생성/수정 요청")
  private BoothMenuRequest request;

  @Schema(description = "메뉴 아이콘 이미지")
  private MultipartFile iconImage;
}
