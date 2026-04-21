/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.manager.exception;

import org.springframework.http.HttpStatus;

import com.skulikelion.festival.global.exception.model.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ManagerErrorCode implements BaseErrorCode {
  MANAGER_NOT_FOUND("MANAGER_4041", "해당 관리자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  BOOTH_NOT_EXIST("MANAGER_4042", "부스 관리자에게 부스가 할당되지 않았습니다.", HttpStatus.NOT_FOUND),
  MANAGER_ALREADY_EXIST("MANAGER_4091", "이미 존재하는 관리자 아이디입니다.", HttpStatus.CONFLICT);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
