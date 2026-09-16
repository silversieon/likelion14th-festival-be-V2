/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.manager.dto.response;

import com.skulikelion.festival.domain.manager.entity.enums.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "ManagerResponse: 관리자 정보 응답 DTO")
public class ManagerResponse {

  @Schema(description = "관리자 식별자", example = "1")
  private Long managerId;

  @Schema(description = "관리자 학과 식별자", example = "12")
  private Long departmentId;

  @Schema(description = "관리자 학과명", example = "소프트웨어학과")
  private String departmentName;

  @Schema(description = "관리자 역할", example = "ADMIN")
  private Role role;
}
