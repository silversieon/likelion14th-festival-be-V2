/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.security;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.exception.model.BaseErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * 부스 자원의 소유권을 검증합니다.
 *
 * <p>같은 로직이 {@code BoothServiceImpl}, {@code BoothMenuServiceImpl}, {@code OrderServiceImpl},
 * {@code LocalOrderSseSubscriber} 네 곳에 복제되어 있던 것을 한 곳으로 모은 것이다.
 *
 * <p>판정 근거가 "매니저를 조회해 {@code Department} enum을 비교"에서 "토큰 클레임의 {@code departmentId}와 부스의 학과 식별자를
 * 비교"로 바뀌었다. 요청당 {@code managers} 조회 1회가 사라진다. (ADR-0001)
 *
 * <p>던질 에러 코드를 호출부가 넘기는 이유는, 기존 코드가 경로마다 서로 다른 코드를 쓰고 있었고(부스/메뉴는 {@code
 * BoothErrorCode.BOOTH_ACCESS_DENIED}, 주문은 {@code OrderErrorCode.BOOTH_ACCESS_DENIED}) 응답 메시지를 바꾸는
 * 것은 프런트 계약 변경이기 때문이다 (api-conventions 5.2).
 *
 * @since 2026.09.14
 */
@Slf4j
@Component
public class BoothOwnershipValidator {

  /**
   * [ 부스 소유 학과이거나 ADMIN이면 통과시키는 검증 메서드 ]
   *
   * <p>부스·메뉴·SSE 구독 경로가 쓴다.
   *
   * @param principal 인증된 요청 주체
   * @param booth 접근 대상 부스
   * @param errorCode 권한이 없을 때 던질 에러 코드
   */
  public void validateOwnerOrAdmin(AuthPrincipal principal, Booth booth, BaseErrorCode errorCode) {
    if (principal.isAdmin()) return;
    validateOwner(principal, booth, errorCode);
  }

  /**
   * [ 부스 소유 학과여야만 통과시키는 검증 메서드 ]
   *
   * <p>주문 경로가 쓴다. <b>ADMIN도 통과하지 못한다</b> — 기존 {@code OrderServiceImpl}의 동작을 그대로 보존한 것이다. 이 불일치가
   * 의도인지 누락인지는 별도 이슈로 다룬다 (LLD-0001 14장 O1).
   *
   * @param principal 인증된 요청 주체
   * @param booth 접근 대상 부스
   * @param errorCode 권한이 없을 때 던질 에러 코드
   */
  public void validateOwner(AuthPrincipal principal, Booth booth, BaseErrorCode errorCode) {
    Long boothDepartmentId = booth.getDepartment().getId();
    if (!boothDepartmentId.equals(principal.departmentId())) {
      log.warn(
          "[Auth] 부스 접근 권한 없음 - 요청자 학과 식별자: {}, 부스 학과 식별자: {}, 요청자 역할: {}",
          principal.departmentId(),
          boothDepartmentId,
          principal.role());
      throw new CustomException(errorCode);
    }
  }
}
