/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.skulikelion.festival.domain.order.entity.enums.OrderCancelReason;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(title = "CanceledOrderResponse: 취소된 주문 응답 DTO")
public class CanceledOrderResponse {

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

  @Schema(description = "취소된 시각", example = "19:00")
  private String cancelTime;

  @Schema(description = "주문 취소 사유", example = "고객 요청")
  private String orderCancelReason;

  @Schema(description = "주문한 메뉴 목록")
  private List<CanceledOrderItemResponse> orderItems;

  @JsonIgnore private LocalDateTime canceledAt;

  @JsonCreator
  public CanceledOrderResponse(
      @JsonProperty("orderId") Long orderId,
      @JsonProperty("tableNumber") Integer tableNumber,
      @JsonProperty("numOfPeople") Integer numOfPeople,
      @JsonProperty("customerName") String customerName,
      @JsonProperty("customerPhoneNumber") String customerPhoneNumber,
      @JsonProperty("totalOrderPrice") Integer totalOrderPrice,
      @JsonProperty("orderDate") String orderDate,
      @JsonProperty("orderTime") String orderTime,
      @JsonProperty("cancelTime") String cancelTime,
      @JsonProperty("orderCancelReason") String orderCancelReason,
      @JsonProperty("orderItems") List<CanceledOrderItemResponse> orderItems) {
    this.orderId = orderId;
    this.tableNumber = tableNumber;
    this.numOfPeople = numOfPeople;
    this.customerName = customerName;
    this.customerPhoneNumber = customerPhoneNumber;
    this.totalOrderPrice = totalOrderPrice;
    this.orderDate = orderDate;
    this.orderTime = orderTime;
    this.cancelTime = cancelTime;
    this.orderCancelReason = orderCancelReason;
    this.orderItems = orderItems;
  }

  public CanceledOrderResponse(
      Long orderId,
      Integer tableNumber,
      Integer numOfPeople,
      String customerName,
      String customerPhoneNumber,
      Integer totalOrderPrice,
      LocalDateTime orderDateTime,
      LocalDateTime canceledTime,
      OrderCancelReason orderCancelReason) {
    this.orderId = orderId;
    this.tableNumber = tableNumber;
    this.numOfPeople = numOfPeople;
    this.customerName = customerName;
    this.customerPhoneNumber = customerPhoneNumber;
    this.totalOrderPrice = totalOrderPrice;
    this.orderDate = orderDateTime.format(DateTimeFormatter.ofPattern("M/d"));
    this.orderTime = orderDateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    this.cancelTime = canceledTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    this.orderCancelReason = orderCancelReason.getDescription();
    this.canceledAt = canceledTime;
  }

  public void addOrderItems(List<CanceledOrderItemResponse> orderItems) {
    this.orderItems = orderItems;
  }
}
