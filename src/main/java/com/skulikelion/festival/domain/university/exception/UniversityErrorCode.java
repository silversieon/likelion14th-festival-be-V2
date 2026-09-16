/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.university.exception;

import org.springframework.http.HttpStatus;

import com.skulikelion.festival.global.exception.model.BaseErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 대학·학과 관련 에러 코드입니다.
 *
 * <p>명명은 api-conventions 5.3의 신규 규칙 {@code <도메인>_<HTTP 3자리><일련 2자리>}를 따른다.
 *
 * @since 2026.09.14
 */
@Getter
@RequiredArgsConstructor
public enum UniversityErrorCode implements BaseErrorCode {
  UNIVERSITY_NAME_REQUIRED("UNIVERSITY_40001", "검색할 학교명을 입력해주세요.", HttpStatus.BAD_REQUEST),
  DEPARTMENT_NOT_IN_UNIVERSITY("UNIVERSITY_40002", "해당 대학에 속한 학과가 아닙니다.", HttpStatus.BAD_REQUEST),
  UNIVERSITY_NOT_FOUND("UNIVERSITY_40401", "해당 대학을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  DEPARTMENT_NOT_FOUND("UNIVERSITY_40402", "해당 학과를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

  private final String code;

  private final String message;

  private final HttpStatus status;
}
