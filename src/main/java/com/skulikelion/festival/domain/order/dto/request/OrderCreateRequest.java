/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.skulikelion.festival.global.enums.Language;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(title = "OrderCreateRequest: 주문 생성 요청 DTO")
public class OrderCreateRequest {

  @NotNull @Positive @Max(999)
  @Schema(description = "테이블 번호", example = "2")
  private Integer tableNumber;

  @NotNull @Positive @Max(99)
  @Schema(description = "인원 수", example = "4")
  private Integer numOfPeople;

  @NotBlank
  @Schema(description = "주문자 이름", example = "윤희준")
  private String customerName;

  @NotBlank
  @Pattern(regexp = "^01[016789]-\\d{3,4}-\\d{4}$")
  @Schema(description = "주문자 전화번호", example = "010-9461-9517")
  private String customerPhoneNumber;

  @NotNull @Positive @Schema(description = "총 주문 금액", example = "55000")
  private Integer totalOrderPrice;

  @NotNull @Schema(description = "언어 타입", example = "KO")
  private Language language;

  @NotEmpty
  @Valid
  @Schema(description = "주문한 메뉴 목록")
  private List<OrderItemCreateRequest> orderItems;

  @JsonIgnore
  @AssertTrue(message = "총 금액이 주문 항목 합계와 일치하지 않습니다")
  public boolean isTotalPriceMatched() {
    if (orderItems == null || orderItems.isEmpty() || totalOrderPrice == null) {
      return true;
    }
    long sum = 0;
    for (OrderItemCreateRequest item : orderItems) {
      if (item == null || item.getMenuPrice() == null || item.getQuantity() == null) {
        return true;
      }
      sum += (long) item.getMenuPrice() * item.getQuantity();
    }
    return sum == totalOrderPrice;
  }
}
