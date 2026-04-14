package com.skulikelion.festival.domain.lostitem.exception;

import com.skulikelion.festival.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LostItemErrorCode implements BaseErrorCode {
  ITEM_NOT_FOUND("LOST_001", "분실물 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  ITEM_ALREADY_DELETED("LOST_002", "이미 삭제된 분실물입니다.", HttpStatus.BAD_REQUEST),
  INVALID_UPDATE_REQUEST("LOST_003", "변경할 항목이 없습니다.", HttpStatus.BAD_REQUEST),
  ITEM_ALREADY_RETURNED("LOST_004", "이미 수령 처리된 분실물입니다.", HttpStatus.CONFLICT),
  ITEM_ALREADY_RESTORED("LOST_005", "이미 복구된 분실물입니다.", HttpStatus.BAD_REQUEST);
  ;

  private final String code;
  private final String message;
  private final HttpStatus status;
}
