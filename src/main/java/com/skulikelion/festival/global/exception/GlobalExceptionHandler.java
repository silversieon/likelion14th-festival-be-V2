/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.skulikelion.festival.global.common.BaseResponse;
import com.skulikelion.festival.global.exception.model.BaseErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  // 커스텀 예외
  @ExceptionHandler(CustomException.class)
  public ResponseEntity<BaseResponse<?>> handleCustomException(CustomException ex) {
    BaseErrorCode errorCode = ex.getErrorCode();
    log.warn("CustomException 발생: {} - {}", errorCode.getCode(), errorCode.getMessage());
    return ResponseEntity.status(errorCode.getStatus())
        .body(BaseResponse.error(errorCode.getStatus().value(), errorCode.getMessage()));
  }

  // Validation 실패
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<BaseResponse<?>> handleValidationException(
      MethodArgumentNotValidException ex) {
    String errorMessages =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> String.format("[%s] %s", e.getField(), e.getDefaultMessage()))
            .collect(Collectors.joining(" / "));
    log.warn("Validation 오류 발생: {}", errorMessages);
    return ResponseEntity.badRequest().body(BaseResponse.error(400, errorMessages));
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<BaseResponse<?>> handleAuthorizationDeniedException(Exception ex) {
    log.warn("AuthorizationDeniedException 오류 발생: {}", ex.getMessage());
    return ResponseEntity.status(403).body(BaseResponse.error(403, "권한 없는 요청 발생"));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<BaseResponse<?>> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException ex) {
    log.warn("MethodArgumentTypeMismatchException 오류 발생: {}", ex.getMessage());
    return ResponseEntity.badRequest().body(BaseResponse.error(400, "유효하지 않은 입력 요청 발생"));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<BaseResponse<?>> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException ex) {
    log.warn("MaxUploadSizeExceededException 오류 발생: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(BaseResponse.error(413, "이미지 용량 제한을 초과했습니다."));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<BaseResponse<Object>> handleHttpMessageNotReadable(
      HttpMessageNotReadableException e) {
    log.warn("[Exception] 잘못된 요청 값 입력 - {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(BaseResponse.error(HttpStatus.BAD_REQUEST.value(), "올바르지 않은 요청 값입니다."));
  }

  @ExceptionHandler(AsyncRequestNotUsableException.class)
  public void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException e) {
    log.warn("[SSE] 클라이언트 연결 끊김 (정상 에러): {}", e.getMessage());
  }

  @ExceptionHandler(AsyncRequestTimeoutException.class)
  public void handleAsyncTimeout(AsyncRequestTimeoutException e) {
    log.warn("[SSE] AsyncRequestTimeoutException 발생 - SSE 타임아웃");
  }

  // 예상치 못한 예외
  @ExceptionHandler(Exception.class)
  public ResponseEntity<BaseResponse<?>> handleException(Exception ex) {
    log.error("Server 오류 발생: ", ex);
    return ResponseEntity.status(GlobalErrorCode.INTERNAL_SERVER_ERROR.getStatus())
        .body(BaseResponse.error(500, GlobalErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
  }
}
