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

  @Schema(description = "관리자 아이디", example = "lion")
  private String username;

  @Schema(description = "관리자 역할", example = "BOOTH_MANAGER")
  private Role role;

  @Schema(description = "담당 부스명")
  private String boothName;
}
