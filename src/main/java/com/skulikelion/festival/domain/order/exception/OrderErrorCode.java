/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.exception;

import org.springframework.http.HttpStatus;

import com.skulikelion.festival.global.exception.model.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderErrorCode implements BaseErrorCode {
  ORDER_TOTAL_PRICE_MISMATCH(
      "ORDER_4001", "주문 총 가격이 메뉴 목록 총 가격과 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
  MENU_PRICE_MISMATCH("ORDER_4002", "주문한 메뉴의 가격과 실제 가격이 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
  ORDER_ITEM_TOTAL_PRICE_MISMATCH(
      "ORDER_4003", "주문 상세 메뉴 가격의 총합이 주문 상세 메뉴 가격의 총합 값과 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
  ORDER_STATUS_CHANGE_FAILED("ORDER_4004", "유효하지 않은 주문 상태 변경입니다.", HttpStatus.BAD_REQUEST),
  ORDER_CANCEL_FAILED("ORDER_4005", "완료된 주문에서 취소 주문으로 변경할 수 없습니다.", HttpStatus.BAD_REQUEST),
  BOOTH_ACCESS_DENIED("ORDER4004", "해당 부스에 대한 접근이 거부되었습니다.", HttpStatus.FORBIDDEN),
  ORDER_NOT_FOUND("ORDER4041", "주문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  ORDER_ITEM_UNIT_NOT_FOUND("ORDER4042", "주문 상세 개별 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
