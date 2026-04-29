/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.idempotency;

import com.skulikelion.festival.domain.order.service.OrderService;

/**
 * 멋쟁이사자처럼 서경대학교 축제 페이지 주문 관련 멱등성 처리 서비스입니다.
 *
 * @since 2026.04.29
 * @see OrderService
 * @author Keum Si Eon
 * @version latest: 1
 */
public interface OrderIdempotencyService {

  /**
   * [ 새로운 요청 여부 확인 메서드 ] 멱등성 키를 보내서 새 요청인지 확인
   *
   * @param idempotencyKey 멱등성 키
   * @return 새 요청 확인 여부 (true면 새 요청)
   */
  boolean isNewRequest(String idempotencyKey);

  /**
   * [ 캐시된 응답 반환 메서드 ] 멱등성 키와 반환 타입을 통해 캐시된 응답 반환
   *
   * @param idempotencyKey 멱등성 키
   * @param responseType 반환 타입
   * @return 캐시된 값
   * @param <T> 주문 생성 응답
   */
  <T> T getCachedResponse(String idempotencyKey, Class<T> responseType);

  /**
   * [ 응답 저장 메서드 ] 멱등성 키를 키값으로 하여 주문 생성 응답을 Redis에 저장 (TTL 10분)
   *
   * @param idempotencyKey 멱등성 키
   * @param response 저장할 주문 생성 응답
   */
  void saveResponse(String idempotencyKey, Object response);

  /**
   * [ 응답 삭제 메서드 ] 멱등성 키를 통해 저장된 키:값 (멱등성 키: 응답) 삭제
   *
   * @param idempotencyKey 멱등성 키
   */
  void deleteKey(String idempotencyKey);
}
