/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.request;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "UpdateBoothMenuSoldOutRequest: 부스 메뉴 품절 여부 변경 요청 DTO")
public class UpdateBoothMenuSoldOutRequest {

  @NotNull(message = "품절 여부를 입력해주세요.") @Schema(description = "품절 여부", example = "true")
  private Boolean soldOut;
}
