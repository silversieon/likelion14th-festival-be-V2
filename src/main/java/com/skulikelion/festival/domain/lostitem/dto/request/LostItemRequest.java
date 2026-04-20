/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LostItemRequest {

  @NotBlank(message = "분실물 이름을 입력하세요.")
  @Schema(description = "분실물 이름", example = "에어팟")
  private String name;

  @Schema(description = "습득 장소")
  @NotBlank(message = "습득 장소를 입력하세요.")
  private String foundPlace;

  @Schema(description = "습득 날짜(yyyy-MM-dd)", example = "2025-05-07")
  @NotNull(message = "습득 날짜를 입력하세요.") private LocalDate foundDate;
}
