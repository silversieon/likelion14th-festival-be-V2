/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.auth.exception;

import org.springframework.http.HttpStatus;

import com.skulikelion.festival.global.exception.model.BaseErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {
  ALREADY_EXIST_DEPARTMENT("AUTH4001", "이미 존재하는 학과 값입니다.", HttpStatus.BAD_REQUEST),
  INCORRECT_PASSWORD("AUTH4002", "비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
  LOGIN_FAIL("AUTH4003", "로그인에 실패했습니다.", HttpStatus.BAD_REQUEST),
  INCORRECT_ADMIN_KEY("AUTH4004", "잘못된 어드민 키 입력", HttpStatus.BAD_REQUEST),
  INVALID_DEPARTMENT("AUTH4005", "존재하지 않는 학과 값입니다.", HttpStatus.NOT_FOUND),
  EXPIRED_ACCESS_TOKEN("AUTH4011", "유효하지 않은 JWT 액세스 토큰입니다.", HttpStatus.UNAUTHORIZED),
  UNAUTHORIZED_TOKEN("AUTH4012", "유효하지 않은 토큰 입력입니다.", HttpStatus.UNAUTHORIZED);

  private final String code;

  private final String message;

  private final HttpStatus status;
}
