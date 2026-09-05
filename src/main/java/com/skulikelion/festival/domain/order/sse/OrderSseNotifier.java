/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse;

import com.skulikelion.festival.domain.order.event.listener.OrderDirectEventListener;
import com.skulikelion.festival.domain.order.event.payload.CanceledOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.CompletedOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.CookingOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.DismissOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.OrderItemUnitStatusPayload;
import com.skulikelion.festival.domain.order.event.payload.WaitingOrderPayload;

/**
 * 멋쟁이사자처럼 서경대학교 축제 페이지 주문 관련 SSE 전송 알림 클래스입니다.
 *
 * @since 2026.04.29
 * @see OrderDirectEventListener
 * @author Keum Si Eon
 * @version latest: 1
 */
public interface OrderSseNotifier {

  /**
   * [ 주문 알림 숫자 증가 전송 메서드 ] 대기 생성, 대기 -> 조리, 취소 -> 대기 등의 주문 이벤트 시에 다른 탭의 관리자들이 +1 알림을 받을 수 있도록 전송
   *
   * @param boothId 이벤트가 발생한 부스
   * @param currentSubscribeType 이벤트가 발생한 구독 타입 (해당 타입을 제외한 다른 타입들에게 전송)
   * @param orderId 이벤트가 발생된 주문 식별자
   */
  void sendOrderIncrementNotification(
      Long boothId, OrderSseSubscribeType currentSubscribeType, Long orderId);

  /**
   * [ 주문 알림 숫자 감소 전송 메서드 ] 대기 -> 조리, 조리 -> 완료 등의 주문 이벤트 시에 다른 탭의 관리자들이 -1 알림을 받을 수 있도록 전송
   *
   * @param booth 이벤트가 발생한 부스
   * @param currentSubscribeType 이벤트를 발생 시킨 구독 타입(해당 타입을 제외한 다른 타입들에게 전송)
   * @param orderId 이벤트가 발생된 주문 식별자
   */
  void sendOrderDecrementNotification(
      Long boothId, OrderSseSubscribeType currentSubscribeType, Long orderId);

  /**
   * [ 주문 탭 이탈 알림 전송 메서드 ] 주문 상태 변경으로 인해 동일 구독 타입의 구독자에게 해당 주문이 현재 탭에서 제외됨을 알림
   *
   * @param booth 이벤트가 발생한 부스
   * @param currentSubscribeType 이벤트를 발생 시킨 구독 타입 (상태를 변경 시킨 탭을 의미, 해당 타입의 구독자에게 전송)
   * @param orderId 탭에서 제외될 주문 ID
   */
  void sendOrderDismissNotification(DismissOrderPayload payload);

  /**
   * [ 대기 중 주문 전송 메서드 ] (주문 생성 요청) 대기 중 주문 내역을 전송
   *
   * @param booth 이벤트가 발생한 부스
   * @param waitingOrderResponse 대기 중 주문 응답
   */
  void sendWaitingOrderEvent(WaitingOrderPayload payload);

  /**
   * [ 조리 중 주문 전송 메서드 ] (대기 > 조리) 주문 상태 변경 시에 조리 중 주문 내역을 전송
   *
   * @param booth 이벤트가 발생한 부스
   * @param cookingOrderResponse 조리 중 주문 응답
   */
  void sendCookingOrderEvent(CookingOrderPayload payload);

  /**
   * [ 완료된 주문 전송 메서드 ] (조리 > 완료) 주문 상태 변경 시에 완료된 주문 내역을 전송
   *
   * @param booth 이벤트가 발생한 부스
   * @param completedOrderResponse 완료된 주문 응답
   */
  void sendCompletedOrderEvent(CompletedOrderPayload payload);

  /**
   * [ 취소된 주문 전송 메서드 ] (대기, 조리 > 취소) 주문 상태 변경 시에 취소된 주문 내역을 전송
   *
   * @param booth 이벤트가 발생한 부스
   * @param canceledOrderResponse 취소된 주문 응답
   */
  void sendCanceledOrderEvent(CanceledOrderPayload payload);

  /**
   * [ 주문 상세 개별 서빙 여부 전송 메서드 ] 주문 상품 개별 서빙 여부 변경 시에 상태 전송
   *
   * @param booth 이벤트가 발생한 부스
   * @param orderItemUnitStatusResponse 주문 상세 개별 상태 응답
   */
  void sendOrderItemUnitStatusEvent(OrderItemUnitStatusPayload payload);
}
