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
  ORDER_MENU_PRICE_MISMATCH("ORDER_4002", "주문한 메뉴의 가격과 실제 가격이 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
  ORDER_ITEM_TOTAL_PRICE_MISMATCH(
      "ORDER_4003", "주문 상세 메뉴 가격의 총합이 주문 상세 메뉴 가격의 총합 값과 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
  ORDER_STATUS_CHANGE_FAILED("ORDER_4004", "유효하지 않은 주문 상태 변경입니다.", HttpStatus.BAD_REQUEST),
  ORDER_CANCEL_FAILED("ORDER_4005", "취소 주문으로 변경할 수 없는 상태입니다.", HttpStatus.BAD_REQUEST),
  ORDER_NOT_USED_BOOTH("ORDER_4006", "주문을 사용하지 않는 부스입니다.", HttpStatus.BAD_REQUEST),
  NOT_TIME_TO_ORDER("ORDER_4007", "주문을 받지 않는 상태입니다.", HttpStatus.BAD_REQUEST),
  ORDER_MENU_SOLD_OUT("ORDER_4007", "품절된 메뉴를 주문하였습니다.", HttpStatus.BAD_REQUEST),
  ORDER_DAY_TYPE_MENU("ORDER_4008", "낮 메뉴는 주문할 수 없습니다.", HttpStatus.BAD_REQUEST),
  ORDER_ITEM_UNIT_STATUS_CHANGE_FAILED(
      "ORDER_4009", "조리 중이 아닌 상품은 서빙 상태를 변경할 수 없습니다.", HttpStatus.BAD_REQUEST),
  ORDER_MENU_TIME_TYPE_NOT_AVAILABLE(
      "ORDER_40010", "현재 주문할 수 없는 시간대의 상품을 주문했습니다.", HttpStatus.BAD_REQUEST),
  BOOTH_ACCESS_DENIED("ORDER4031", "해당 부스에 대한 접근이 거부되었습니다.", HttpStatus.FORBIDDEN),
  ORDER_NOT_FOUND("ORDER4041", "주문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  ORDER_ITEM_UNIT_NOT_FOUND("ORDER4042", "주문 상세 개별 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  ORDER_TIME_BOOTH_NOT_FOUND("ORDER4043", "해당 날짜에 운영하는 부스 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  ORDER_ALREADY_PROCESSING("ORDER4091", "이미 처리 중인 주문입니다.", HttpStatus.CONFLICT),
  ORDER_REVERT_NOT_ALLOWED("ORDER4092", "지난 날짜의 주문은 되돌릴 수 없습니다.", HttpStatus.CONFLICT),
  ORDER_IDEMPOTENCY_KEY_EXPIRED("ORDER4101", "멱등성 키가 만료되었습니다. 새로운 요청을 보내주세요.", HttpStatus.GONE),
  INVALID_SUBSCRIBE_TYPE_CONVERSION(
      "ORDER5001", "주문 상태로 변환할 수 없는 구독 타입입니다.", HttpStatus.INTERNAL_SERVER_ERROR);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
