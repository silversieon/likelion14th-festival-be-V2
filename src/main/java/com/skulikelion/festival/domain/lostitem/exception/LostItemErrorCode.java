/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.exception;

import org.springframework.http.HttpStatus;

import com.skulikelion.festival.global.exception.model.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LostItemErrorCode implements BaseErrorCode {
  INVALID_UPDATE_REQUEST("LOST_4001", "변경할 항목이 없습니다.", HttpStatus.BAD_REQUEST),
  INVALID_PAGE_REQUEST("LOST_4002", "페이지 번호는 0 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
  INVALID_PAGE_SIZE_REQUEST("LOST_4003", "페이지 크기는 1 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
  INVALID_DATE_FORMAT("LOST_4004", "날짜 형식이 올바르지 않습니다. (yyyy-MM-dd)", HttpStatus.BAD_REQUEST),
  LOST_ITEM_IMAGE_REQUIRED("LOST_4005", "분실물 이미지는 최소 1장 이상 등록해야 합니다.", HttpStatus.BAD_REQUEST),
  LOST_ITEM_IMAGE_COUNT_INVALID(
      "LOST_4006", "분실물 이미지는 최소 1장, 최대 4장까지 등록할 수 있습니다.", HttpStatus.BAD_REQUEST),
  INVALID_FESTIVAL_DATE("LOST_4007", "축제 기간 내 날짜만 선택 가능합니다.", HttpStatus.BAD_REQUEST),

  ITEM_NOT_FOUND("LOST_4041", "분실물 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

  ITEM_ALREADY_RETURNED("LOST_4091", "이미 수령 처리된 분실물입니다.", HttpStatus.CONFLICT);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
