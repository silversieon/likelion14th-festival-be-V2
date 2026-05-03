/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.request.menu;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "BoothMenuListRequest: 부스 메뉴 목록 생성 요청 DTO")
public class BoothMenuListRequest {

  @Valid
  @NotEmpty(message = "메뉴 생성 정보를 입력해주세요.")
  @Schema(description = "메뉴 생성 정보 목록")
  private List<BoothMenuRequest> menus;
}
