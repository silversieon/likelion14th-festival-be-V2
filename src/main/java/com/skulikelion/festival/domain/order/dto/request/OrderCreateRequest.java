/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.dto.request;

import java.util.List;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.skulikelion.festival.global.enums.Language;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(title = "OrderCreateRequest: 주문 생성 요청 DTO")
public class OrderCreateRequest {

  @Digits(integer = 3, fraction = 0)
  @Schema(description = "테이블 번호", example = "2")
  private Integer tableNumber;

  @Digits(integer = 2, fraction = 0)
  @Schema(description = "인원 수", example = "4")
  private Integer numOfPeople;

  @NotBlank
  @Schema(description = "주문자 이름", example = "윤희준")
  private String customerName;

  @NotBlank
  @Pattern(regexp = "^01[016789]-\\d{3,4}-\\d{4}$")
  @Schema(description = "주문자 전화번호", example = "010-9461-9517")
  private String customerPhoneNumber;

  @Digits(integer = 9, fraction = 0)
  @Schema(description = "총 주문 금액", example = "55000")
  private Integer totalOrderPrice;

  @NotNull @Schema(description = "언어 타입", example = "KO")
  private Language language;

  @Schema(description = "주문한 메뉴 목록")
  private List<OrderItemCreateRequest> orderItems;
}
