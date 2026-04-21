/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.manager.dto.request;

import jakarta.validation.constraints.NotBlank;

import com.skulikelion.festival.domain.manager.entity.enums.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(title = "CreateManagerRequest: 관리자 생성 요청 DTO")
public class CreateManagerRequest {

  @NotBlank
  @Schema(description = "관리자 아이디", example = "likelion")
  private String username;

  @NotBlank
  @Schema(description = "관리자 비밀번호", example = "likelion14!")
  private String password;

  @Schema(description = "관리자 역할")
  private Role role;

  @Schema(description = "부스 식별자")
  private Long boothId;
}
