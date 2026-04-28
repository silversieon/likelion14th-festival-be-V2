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

  BOOTH_NOT_FOUND("BOOTH_4041", "부스 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  BOOTH_TRANSLATION_NOT_FOUND("BOOTH_4042", "부스 번역 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

  BOOTH_ALREADY_EXISTS("BOOTH_4091", "이미 해당 학과의 부스가 존재합니다.", HttpStatus.CONFLICT);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
