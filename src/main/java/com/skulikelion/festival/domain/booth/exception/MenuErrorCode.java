package com.skulikelion.festival.domain.booth.exception;

import com.skulikelion.festival.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MenuErrorCode implements BaseErrorCode {
  MENU_ALREADY_EXISTS("MENU_001", "이미 존재하는 메뉴입니다.", HttpStatus.CONFLICT),
  MENU_NOT_FOUND("MENU_002", "메뉴 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
