/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.skulikelion.festival.domain.lostitem.controller.LostItemController;
import com.skulikelion.festival.global.common.BaseResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice(assignableTypes = LostItemController.class)
public class LostItemExceptionHandler {

  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<BaseResponse<?>> handleMissingPart(MissingServletRequestPartException ex) {

    String partName = ex.getRequestPartName();

    String message =
        switch (partName) {
          case "request" -> "분실물 정보를 입력해주세요.";
          case "images" -> "이미지는 최소 1장 이상 등록해야 합니다.";
          default -> "필수 항목을 모두 입력해주세요.";
        };

    log.warn("[LostItem] 필수 multipart 누락 - {} / {}", partName, message);

    return ResponseEntity.badRequest().body(BaseResponse.error(400, message));
  }
}
