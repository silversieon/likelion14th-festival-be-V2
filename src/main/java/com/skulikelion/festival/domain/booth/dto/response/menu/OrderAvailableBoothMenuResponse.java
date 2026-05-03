/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.response.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "OrderAvailableBoothMenuResponse: 주문 가능 부스 메뉴 응답 DTO")
public class OrderAvailableBoothMenuResponse {

  @Schema(description = "메뉴 식별자", example = "1")
  private Long menuId;

  @Schema(description = "아이콘 이미지 URL")
  private String iconImageUrl;

  @Schema(description = "메뉴명")
  private String name;

  @Schema(description = "메뉴 설명")
  private String description;

  @Schema(description = "가격", example = "11000")
  private Integer price;

  @Schema(description = "품절 여부", example = "false")
  private Boolean soldOut;
}
