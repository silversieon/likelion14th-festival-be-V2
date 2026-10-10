/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.statistics.exception;

import org.springframework.http.HttpStatus;

import com.skulikelion.festival.global.exception.model.BaseErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 운영진 통계 관련 에러 코드입니다.
 *
 * <p>명명은 api-conventions 5.3의 신규 규칙 {@code <도메인>_<HTTP 3자리><일련 2자리>}를 따른다.
 *
 * @since 2026.10.10
 */
@Getter
@RequiredArgsConstructor
public enum StatisticsErrorCode implements BaseErrorCode {
  STATISTICS_DATE_RANGE_REQUIRED(
      "STATISTICS_40001", "조회 시작일과 종료일을 모두 입력해주세요.", HttpStatus.BAD_REQUEST),
  STATISTICS_INVALID_DATE_RANGE(
      "STATISTICS_40002", "시작일은 종료일보다 늦을 수 없습니다.", HttpStatus.BAD_REQUEST),
  STATISTICS_UNIVERSITY_NAME_REQUIRED(
      "STATISTICS_40003", "검색할 학교명을 입력해주세요.", HttpStatus.BAD_REQUEST);

  private final String code;

  private final String message;

  private final HttpStatus status;
}
