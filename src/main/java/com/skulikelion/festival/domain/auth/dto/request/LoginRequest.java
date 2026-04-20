/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
@Schema(title = "LoginRequest: 로그인 요청 DTO")
public class LoginRequest {

  @NotBlank
  @Schema(description = "사용자 아이디", example = "likelion")
  private String username;

  @NotBlank
  @Schema(description = "사용자 비밀번호", example = "lion1234!")
  private String password;
}
