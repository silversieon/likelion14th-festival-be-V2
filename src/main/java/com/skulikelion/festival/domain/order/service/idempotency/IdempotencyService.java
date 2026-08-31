/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.idempotency;

import java.util.function.Supplier;

/**
 * 멋쟁이사자처럼 서경대학교 축제 페이지 주문 관련 멱등성 처리 서비스입니다.
 *
 * @since 2026.04.29
 * @version 2026.07.17 - 멱등성 관련 로직을 환경별로 분리
 * @author Keum Si Eon
 * @version latest: 1
 */
public interface IdempotencyService {

  <T> T executeIdempotent(String idempotencyKey, Supplier<T> processor, Class<T> responseType);
}
