/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderItemUnitStatusResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;
import com.skulikelion.festival.domain.order.enums.SseSubscribeType;
import com.skulikelion.festival.domain.order.service.OrderService;

/**
 * 멋쟁이사자처럼 서경대학교 축제 페이지 주문 관련 SSE 처리 서비스입니다.
 *
 * @since 2026.04.29
 * @see OrderService
 * @author Keum Si Eon
 * @version latest: 1
 */
public interface OrderSseService {

  /**
   * [ 주문 관련 구독 메서드 ] 학과명(부스 식별자로 치환), 구독 타입을 통해서 부스 관리 화면의 각 탭별 사용자를 구독
   *
   * @param departmentName 학과명
   * @param sseSubscribeType 구독 타입 (부스 관리 탭 5가지)
   * @return SseEmitter 객체
   */
  SseEmitter subscribeOrder(String departmentName, SseSubscribeType sseSubscribeType);

  /**
   * [ 주문 알림 숫자 증가 전송 메서드 ] 대기 생성, 대기 -> 조리, 취소 -> 대기 등 주문 이벤트 시에 다른 탭의 관리자들이 +1 할 수 있게 알림 전송
   *
   * @param booth 이벤트가 발생한 부스
   * @param currentSubscribeType 이벤트가 발생한 구독 타입(해당 타입을 제외한 다른 타입들에게 전송)
   */
  void sendOrderIncrementNotification(Booth booth, SseSubscribeType currentSubscribeType);

  /**
   * [ 주문 알림 숫자 감소 전송 메서드 ] 대기 -> 조리, 조리 -> 완료 등 주문 이벤트 시에 다른 탭의 관리자들이 알림 -1 할 수 있게 알림 전송
   *
   * @param booth 이벤트가 발생한 부스
   * @param currentSubscribeType 이벤트를 발생 시킨 구독 타입(해당 타입을 제외한 다른 타입들에게 전송)
   */
  void sendOrderDecrementNotification(Booth booth, SseSubscribeType currentSubscribeType);

  /**
   * [ 주문 탭 이탈 알림 전송 메서드 ] 주문 상태 변경으로 인해 동일 구독 타입의 구독자에게 해당 주문이 현재 탭에서 제외됨을 알림
   *
   * @param booth 이벤트가 발생한 부스
   * @param currentSubscribeType 이벤트를 발생 시킨 구독 타입 (상태를 변경 시킨 탭을 의미, 해당 타입의 구독자에게 전송)
   * @param orderId 탭에서 제외될 주문 ID
   */
  void sendOrderDismissNotification(
      Booth booth, SseSubscribeType currentSubscribeType, Long orderId);

  /**
   * [ 대기 중 주문 전송 메서드 ] (주문 생성 요청) 대기 중 주문 내역을 전송
   *
   * @param booth 이벤트가 발생한 부스
   * @param waitingOrderResponse 대기 중 주문 응답
   */
  void sendWaitingOrderEvent(Booth booth, WaitingOrderResponse waitingOrderResponse);

  /**
   * [ 조리 중 주문 전송 메서드 ] (대기 > 조리) 주문 상태 변경 시에 조리 중 주문 내역을 전송
   *
   * @param booth 이벤트가 발생한 부스
   * @param cookingOrderResponse 조리 중 주문 응답
   */
  void sendCookingOrderEvent(Booth booth, CookingOrderResponse cookingOrderResponse);

  /**
   * [ 완료된 주문 전송 메서드 ] (조리 > 완료) 주문 상태 변경 시에 완료된 주문 내역을 전송
   *
   * @param booth 이벤트가 발생한 부스
   * @param completedOrderResponse 완료된 주문 응답
   */
  void sendCompletedOrderEvent(Booth booth, CompletedOrderResponse completedOrderResponse);

  /**
   * [ 취소된 주문 전송 메서드 ] (대기, 조리 > 취소) 주문 상태 변경 시에 취소된 주문 내역을 전송
   *
   * @param booth 이벤트가 발생한 부스
   * @param canceledOrderResponse 취소된 주문 응답
   */
  void sendCanceledOrderEvent(Booth booth, CanceledOrderResponse canceledOrderResponse);

  /**
   * [ 주문 상세 개별 서빙 여부 전송 메서드 ] 주문 상품 개별 서빙 여부 변경 시에 상태 전송
   *
   * @param booth 이벤트가 발생한 부스
   * @param orderItemUnitStatusResponse 주문 상세 개별 상태 응답
   */
  void sendOrderItemUnitStatusEvent(
      Booth booth, OrderItemUnitStatusResponse orderItemUnitStatusResponse);
}
