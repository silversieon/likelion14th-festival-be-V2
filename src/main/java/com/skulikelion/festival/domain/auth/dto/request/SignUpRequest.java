/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.skulikelion.festival.domain.manager.entity.enums.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
@Schema(title = "SignUpRequest: 회원가입 요청 DTO")
public class SignUpRequest {

  @NotBlank
  @Schema(description = "사용자 학과", example = "SKULIKELION")
  private String departmentName;

  @NotBlank
  @Schema(description = "사용자 비밀번호", example = "lion1234!")
  private String password;

  @NotNull @Schema(description = "사용자 권한", example = "BOOTH_MANAGER")
  private Role role;

  @NotBlank
  @Schema(description = "관리자 키")
  private String adminKey;
}
