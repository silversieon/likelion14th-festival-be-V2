/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "OrderAvailableBoothMenuGroupResponse: 주문 가능 부스 메뉴 카테고리별 응답 DTO")
public class OrderAvailableBoothMenuGroupResponse {

  @Schema(description = "메인 메뉴 목록")
  private List<OrderAvailableBoothMenuResponse> main;

  @Schema(description = "사이드 메뉴 목록")
  private List<OrderAvailableBoothMenuResponse> side;

  @Schema(description = "음료 메뉴 목록")
  private List<OrderAvailableBoothMenuResponse> drink;
}
