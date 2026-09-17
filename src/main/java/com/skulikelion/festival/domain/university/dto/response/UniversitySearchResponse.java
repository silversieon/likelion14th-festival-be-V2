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
@Schema(title = "UniversitySearchResponse: 학교명 검색 응답 DTO")
public class UniversitySearchResponse {

  @Schema(description = "학교 식별자", example = "1")
  private Long universityId;

  @Schema(description = "학교명", example = "서경대학교")
  private String universityName;
}
