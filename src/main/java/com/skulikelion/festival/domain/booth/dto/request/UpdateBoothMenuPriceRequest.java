/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "UpdateBoothMenuPriceRequest: 부스 메뉴 가격 수정 요청 DTO")
public class UpdateBoothMenuPriceRequest {

  @NotNull(message = "가격을 입력해주세요.") @PositiveOrZero(message = "가격은 0원 이상으로 입력해주세요.")
  @Schema(description = "가격", example = "12000")
  private Integer price;
}
