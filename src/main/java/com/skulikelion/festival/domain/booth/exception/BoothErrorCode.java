/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.exception;

import org.springframework.http.HttpStatus;

import com.skulikelion.festival.global.exception.model.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BoothErrorCode implements BaseErrorCode {
  BOOTH_DETAIL_IMAGE_LIMIT_EXCEEDED(
      "BOOTH_4001", "부스 상세 이미지는 최대 3개까지 등록할 수 있습니다.", HttpStatus.BAD_REQUEST),
  INVALID_TIME_FORMAT("BOOTH_4002", "시간 형식은 HH:mm 이어야 합니다.", HttpStatus.BAD_REQUEST),
  BOOTH_TRANSLATION_REQUIRED("BOOTH_4003", "모든 언어의 부스 번역 정보를 입력해주세요.", HttpStatus.BAD_REQUEST),
  BOOTH_TRANSLATION_DUPLICATED("BOOTH_4004", "중복된 언어의 부스 번역 정보가 있습니다.", HttpStatus.BAD_REQUEST),
  INVALID_OPERATION_DATE_FORMAT(
      "BOOTH_4005", "운영 날짜 형식은 yyyy-MM-dd 이어야 합니다.", HttpStatus.BAD_REQUEST),
  BOOTH_OPERATION_REQUIRED("BOOTH_4006", "부스 운영 정보는 3개 이상 입력해주세요.", HttpStatus.BAD_REQUEST),
  BOOTH_OPERATION_DUPLICATED("BOOTH_4007", "중복된 날짜의 부스 운영 정보가 있습니다.", HttpStatus.BAD_REQUEST),
  BOOTH_OPERATION_TIME_REQUIRED("BOOTH_4008", "운영 타입에 맞는 운영 시간을 입력해주세요.", HttpStatus.BAD_REQUEST),
  BOOTH_MENU_ICON_IMAGE_COUNT_MISMATCH(
      "BOOTH_4009", "메뉴 개수와 아이콘 이미지 개수가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),

  BOOTH_ACCESS_DENIED("BOOTH_4031", "해당 부스에 대한 접근이 거부되었습니다.", HttpStatus.FORBIDDEN),

  BOOTH_NOT_FOUND("BOOTH_4041", "부스 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  BOOTH_TRANSLATION_NOT_FOUND("BOOTH_4042", "부스 번역 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  BOOTH_OPERATION_NOT_FOUND("BOOTH_4043", "부스 운영 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  BOOTH_MENU_NOT_FOUND("BOOTH_4044", "부스 메뉴 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

  BOOTH_ALREADY_EXISTS("BOOTH_4091", "이미 해당 학과의 부스가 존재합니다.", HttpStatus.CONFLICT);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
