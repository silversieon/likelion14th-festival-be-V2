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
  // AUTH4001(ALREADY_EXIST_DEPARTMENT), AUTH4005(INVALID_DEPARTMENT)는 Department enum 제거와 함께
  // 폐기되었다. 번호는 재사용하지 않는다 (api-conventions 5.3, ADR-0001 / LLD-0001 12.3).
  INCORRECT_PASSWORD("AUTH4002", "비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
  LOGIN_FAIL("AUTH4003", "로그인에 실패했습니다.", HttpStatus.BAD_REQUEST),
  INCORRECT_ADMIN_KEY("AUTH4004", "잘못된 어드민 키 입력", HttpStatus.BAD_REQUEST),
  EXPIRED_ACCESS_TOKEN("AUTH4011", "유효하지 않은 JWT 액세스 토큰입니다.", HttpStatus.UNAUTHORIZED),
  UNAUTHORIZED_TOKEN("AUTH4012", "유효하지 않은 토큰 입력입니다.", HttpStatus.UNAUTHORIZED),
  ALREADY_EXIST_MANAGER("AUTH4091", "이미 해당 학과의 관리자 계정이 존재합니다.", HttpStatus.CONFLICT);

  private final String code;

  private final String message;

  private final HttpStatus status;
}
