/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.university.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "DepartmentResponse: 학과 목록 응답 DTO")
public class DepartmentResponse {

  @Schema(description = "학과 식별자", example = "12")
  private Long departmentId;

  @Schema(description = "학과명", example = "소프트웨어학과")
  private String departmentName;
}
