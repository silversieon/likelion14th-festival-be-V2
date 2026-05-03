/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.request.menu;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "BoothMenuBulkMultipartBody: 부스 메뉴 벌크 multipart 요청 DTO")
public class BoothMenuBulkMultipartBody {

  @Schema(description = "부스 메뉴 생성 요청 목록")
  private BoothMenuListRequest request;

  @Schema(description = "메뉴 아이콘 이미지 목록")
  private List<MultipartFile> iconImages;
}
