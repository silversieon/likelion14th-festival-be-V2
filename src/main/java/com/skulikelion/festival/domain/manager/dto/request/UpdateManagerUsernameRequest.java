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
@Schema(title = "UpdateManagerUsernameRequest: 관리자 아이디 수정 DTO")
public class UpdateManagerUsernameRequest {

  @NotBlank
  @Schema(description = "변경할 관리자 아이디", example = "likelion")
  private String username;
}
