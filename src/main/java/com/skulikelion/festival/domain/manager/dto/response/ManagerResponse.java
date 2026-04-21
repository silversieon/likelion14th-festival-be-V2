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

  private Long managerId;

  private String username;

  private Role role;

  private String boothName;
}
