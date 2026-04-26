/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(title = "FinishedOrderResponse: 완료된 주문 응답 DTO")
public class CompletedOrderResponse {

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

  @Schema(description = "주문 총 가격", example = "56000")
  private Integer totalOrderPrice;

  @Schema(description = "주문 날짜", example = "5/13")
  private String orderDate;

  @Schema(description = "주문 시각", example = "18:30")
  private String orderTime;

  @Schema(description = "완료된 시각", example = "19:00")
  private String completeTime;

  @Schema(description = "주문한 메뉴 목록")
  private List<CompletedOrderItemResponse> orderItems;

  public CompletedOrderResponse(
      Long orderId,
      Integer tableNumber,
      Integer numOfPeople,
      String customerName,
      String customerPhoneNumber,
      Integer totalOrderPrice,
      LocalDateTime orderDateTime,
      LocalDateTime completedTime) {
    this.orderId = orderId;
    this.tableNumber = tableNumber;
    this.numOfPeople = numOfPeople;
    this.customerName = customerName;
    this.customerPhoneNumber = customerPhoneNumber;
    this.totalOrderPrice = totalOrderPrice;
    this.orderDate = orderDateTime.format(DateTimeFormatter.ofPattern("M/d"));
    this.orderTime = orderDateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    this.completeTime = completedTime.format(DateTimeFormatter.ofPattern("HH:mm"));
  }

  public void addOrderItems(List<CompletedOrderItemResponse> orderItems) {
    this.orderItems = orderItems;
  }
}
