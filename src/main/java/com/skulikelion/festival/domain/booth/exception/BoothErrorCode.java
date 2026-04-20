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
  BOOTH_ALREADY_EXISTS("BOOTH_001", "이미 존재하는 부스입니다.", HttpStatus.CONFLICT),
  BOOTH_NOT_FOUND("BOOTH_002", "부스 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  INVALID_PASSWORD("BOOTH_003", "잘못된 비밀번호입니다.", HttpStatus.FORBIDDEN);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
