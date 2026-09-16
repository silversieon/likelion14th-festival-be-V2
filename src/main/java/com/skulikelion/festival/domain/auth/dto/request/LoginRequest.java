/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 요청 DTO입니다.
 *
 * <p>전국 확장으로 서비스가 한 학교에 종속되지 않게 되면서, 학과명 하나가 아니라 <b>대학 + 학과</b>로 계정을 특정한다. (ADR-0001)
 */
@Getter
@AllArgsConstructor
@Builder
@Schema(title = "LoginRequest: 로그인 요청 DTO")
public class LoginRequest {

  @NotNull @Schema(description = "대학 식별자", example = "1")
  private Long universityId;

  @NotNull @Schema(description = "학과 식별자", example = "12")
  private Long departmentId;

  @NotBlank
  @Schema(description = "사용자 비밀번호", example = "lion1234!")
  private String password;
}
