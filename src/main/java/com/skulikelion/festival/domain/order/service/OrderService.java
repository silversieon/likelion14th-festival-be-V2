/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service;

import java.time.LocalDate;
import java.util.List;

import com.skulikelion.festival.domain.order.dto.request.CookingOrderCursor;
import com.skulikelion.festival.domain.order.dto.request.OrderCreateRequest;
import com.skulikelion.festival.domain.order.dto.request.OrderItemUnitUpdateRequest;
import com.skulikelion.festival.domain.order.dto.request.WaitingOrderCursor;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderResponse;
import com.skulikelion.festival.domain.order.dto.response.SalesResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;
import com.skulikelion.festival.domain.order.entity.enums.OrderCancelReason;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;
import com.skulikelion.festival.global.common.pagenation.CursorPageResponse;

/**
 * 멋쟁이사자처럼 서경대학교 축제 페이지 주문 관련 처리 서비스입니다.
 *
 * @since 2026.04.29
 * @see com.skulikelion.festival.domain.order.controller.OrderController
 * @author Keum Si Eon
 * @version latest: 1
 */
public interface OrderService {

  /**
   * [ 주문 생성 메서드 ] 사용자가 주문 생성 요청 시 주문을 DB에 저장하고, 이벤트를 전송
   *
   * @param boothId 부스 식별자
   * @param idempotencyKey 멱등성 키 (중복 요청 방지)
   * @param request 주문 생성 요청 정보
   * @return 생성된 주문 응답
   */
  OrderResponse createOrder(Long boothId, String idempotencyKey, OrderCreateRequest request);

  /**
   * [ 대기 중 주문 목록 조회 메서드 ] 대기 중 주문 목록 조회
   *
   * @param departmentName 학과명
   * @return 대기 중 주문 목록
   */
  CursorPageResponse<WaitingOrderResponse> getWaitingOrders(
      String departmentName, WaitingOrderCursor cursor, Integer size);

  /**
   * [ 조리 중 주문 목록 조회 메서드 ] 조리 중 주문 목록 조회
   *
   * @param departmentName 학과명
   * @return 조리 중 주문 목록
   */
  CursorPageResponse<CookingOrderResponse> getCookingOrders(String departmentName, CookingOrderCursor cursor, Integer size);

  /**
   * [ 완료된 주문 목록 조회 메서드 ] 완료된 주문 목록 조회
   *
   * @param departmentName 학과명
   * @param orderDate 조회할 날짜 (null 시에 전체 조회)
   * @param keyword 검색어 (이름, 전화번호)
   * @return 완료된 주문 목록
   */
  List<CompletedOrderResponse> getCompletedOrders(
      String departmentName, LocalDate orderDate, String keyword);

  /**
   * [ 취소된 주문 조회 메서드 ] 취소된 주문 목록 조회
   *
   * @param departmentName 학과명
   * @param orderDate 조회할 날짜 (null 시에 전체 조회)
   * @param keyword 검색어 (이름, 전화번호)
   * @return 취소된 주문 목록
   */
  List<CanceledOrderResponse> getCanceledOrders(
      String departmentName, LocalDate orderDate, String keyword);

  /**
   * [ 매출액 조회 메서드 ] 날짜별 매출액 조회
   *
   * @param departmentName 학과명
   * @param date 조회할 날짜
   * @return 매출액 응답 객체
   */
  SalesResponse getSales(String departmentName, LocalDate date);

  /**
   * [ 주문 상태 변경 메서드 ] 각 주문의 상태 변경 (대기 > 조리, 조리 > 완료 가능)
   *
   * @param departmentName 학과명
   * @param orderId 상태를 변경할 주문 식별자
   * @param orderStatus 변경할 주문 상태
   */
  void updateOrderStatus(String departmentName, Long orderId, OrderStatus orderStatus);

  /**
   * [ 주문 취소 메서드 ] 주문을 취소 (대기 > 취소, 조리 > 취소 가능)
   *
   * @param departmentName 학과명
   * @param orderId 주문을 취소할 주문 식별자
   * @param orderCancelReason 주문 취소 사유
   */
  void cancelOrder(String departmentName, Long orderId, OrderCancelReason orderCancelReason);

  /**
   * [ 주문 상세 개별 상태 변경 메서드 ] 주문 상품 개별 서빙(또는 완료) 여부 변경
   *
   * @param departmentName 학과명
   * @param orderItemUnitId 상태를 변경할 주문 상세 개별 식별자
   * @param request 주문 상세 개별 변경 요청 객체
   */
  void updateServedStatus(
      String departmentName, Long orderItemUnitId, OrderItemUnitUpdateRequest request);
}
