/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(title = "WaitingOrderResponse: 대기 중인 주문 응답 DTO")
public class WaitingOrderResponse {

  @Schema(description = "주문 식별자", example = "1")
  private Long orderId;

  @Schema(description = "테이블 번호", example = "2")
  private Integer tableNumber;

  @Schema(description = "인원 수", example = "4")
  private Integer numOfPeople;

  @Schema(description = "주문자 이름", example = "윤희준")
  private String customerName;

  @Schema(description = "주문자 전화번호", example = "010-9461-9517")
  private String customerPhoneNumber;

  @Schema(description = "주문 시각", example = "18:30")
  private String orderTime;

  @Schema(description = "주문 총 가격", example = "56000")
  private Integer totalOrderPrice;

  @Schema(description = "주문한 메뉴 상세 목록")
  private List<WaitingOrderItemResponse> orderItems;

  @JsonIgnore private LocalDateTime createdAt;

  public WaitingOrderResponse(
      Long orderId,
      Integer tableNumber,
      Integer numOfPeople,
      String customerName,
      String customerPhoneNumber,
      LocalDateTime orderTime,
      Integer totalOrderPrice) {
    this.orderId = orderId;
    this.tableNumber = tableNumber;
    this.numOfPeople = numOfPeople;
    this.customerName = customerName;
    this.customerPhoneNumber = customerPhoneNumber;
    this.orderTime = orderTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    this.totalOrderPrice = totalOrderPrice;
    this.createdAt = orderTime;
  }

  public void addOrderItems(List<WaitingOrderItemResponse> orderItems) {
    this.orderItems = orderItems;
  }
}
