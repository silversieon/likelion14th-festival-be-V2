/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(title = "OrderCreateRequest: 주문 생성 요청 DTO")
public class OrderCreateRequest {

  @Schema(description = "테이블 번호", example = "2")
  private Integer tableNumber;

  @Schema(description = "인원 수", example = "4")
  private Integer numOfPeople;

  @Schema(description = "주문자 이름", example = "윤희준")
  private String customerName;

  @Schema(description = "주문자 전화번호", example = "010-9461-9517")
  private String customerPhoneNumber;

  @Schema(description = "총 주문 금액", example = "55000")
  private Integer totalOrderPrice;

  @Schema(description = "주문한 메뉴 목록")
  private List<OrderItemCreateRequest> orderItems;
}
