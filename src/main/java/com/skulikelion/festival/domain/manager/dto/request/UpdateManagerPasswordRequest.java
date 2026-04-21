/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.manager.dto.request;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(title = "UpdateManagerPasswordRequest: 관리자 비밀번호 수정 DTO")
public class UpdateManagerPasswordRequest {

  @NotBlank
  @Schema(description = "관리자 비밀번호", example = "likelion14!")
  private String password;
}
