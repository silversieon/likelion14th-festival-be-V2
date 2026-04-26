/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(title = "OrderResponse: 주문 응답 DTO")
public class OrderResponse {

  @Schema(description = "주문 식별자", example = "1")
  private Long orderId;

  @Schema(description = "주문자 이름", example = "윤희준")
  private String customerName;

  @Schema(description = "주문자 전화번호", example = "010-9461-9517")
  private String customerPhoneNumber;

  @Schema(description = "주문 시각", example = "18:30:00")
  private String orderTime;

  @Schema(description = "주문한 메뉴 목록")
  private List<OrderItemResponse> orderItems;

  @Schema(description = "주문 총 가격", example = "56000")
  private Integer totalOrderPrice;

  @Schema(description = "은행 이름", example = "카카오뱅크")
  private String bankName;

  @Schema(description = "예금주", example = "김멋사")
  private String accountName;

  @Schema(description = "계좌번호", example = "111111100000")
  private String accountNumber;
}
