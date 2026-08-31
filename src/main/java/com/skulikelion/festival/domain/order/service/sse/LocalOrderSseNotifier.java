/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.sse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.order.dto.event.DismissOrderIdEvent;
import com.skulikelion.festival.domain.order.dto.event.OrderCountNotification;
import com.skulikelion.festival.domain.order.dto.response.*;
import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.domain.order.service.sse.store.LocalOrderSseEmitterStore;
import com.skulikelion.festival.global.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalOrderSseNotifier implements OrderSseNotifier {

  private final LocalOrderSseEmitterStore store;

  @Override
  public void sendOrderIncrementNotification(
      Booth booth, SseSubscribeType currentSubscribeType, Long orderId) {
    Map<SseSubscribeType, List<SseEmitter>> statusMap = store.findByBoothId(booth.getId());

    statusMap.entrySet().stream()
        .filter(entry -> entry.getKey() != currentSubscribeType)
        .forEach(
            entry -> {
              SseSubscribeType subscribedType = entry.getKey();
              entry
                  .getValue()
                  .forEach(
                      emitter -> {
                        try {
                          emitter.send(
                              SseEmitter.event()
                                  .name("orderIncrementNotification")
                                  .data(
                                      OrderCountNotification.builder()
                                          .orderStatus(
                                              currentSubscribeType
                                                  .toOrderStatus()
                                                  .orElseThrow(
                                                      () ->
                                                          new CustomException(
                                                              OrderErrorCode
                                                                  .INVALID_SUBSCRIBE_TYPE_CONVERSION)))
                                          .orderId(orderId)
                                          .build()));
                          log.debug(
                              "[OrderSseService] 주문 상태 변경에 의한 증가 알림 전송 성공 - 학과명: {}, 수신 제외된 타입: {}, 수신된 타입: {}",
                              booth.getDepartment().getDescription(),
                              currentSubscribeType,
                              subscribedType);
                        } catch (IOException e) {
                          log.warn(
                              "[OrderSseService] 주문 상태 변경에 의한 증가 알림 전송 실패 - 학과명: {}, 수신되었어야 할 타입: {}",
                              booth.getDepartment().getDescription(),
                              subscribedType);
                          store.remove(booth.getId(), subscribedType, emitter);
                        }
                      });
            });
  }

  @Override
  public void sendOrderDecrementNotification(
      Booth booth, SseSubscribeType currentSubscribeType, Long orderId) {
    Map<SseSubscribeType, List<SseEmitter>> statusMap = store.findByBoothId(booth.getId());

    statusMap.entrySet().stream()
        .filter(entry -> entry.getKey() != currentSubscribeType)
        .forEach(
            entry -> {
              SseSubscribeType subscribedType = entry.getKey();
              entry
                  .getValue()
                  .forEach(
                      emitter -> {
                        try {
                          emitter.send(
                              SseEmitter.event()
                                  .name("orderDecrementNotification")
                                  .data(
                                      OrderCountNotification.builder()
                                          .orderStatus(
                                              currentSubscribeType
                                                  .toOrderStatus()
                                                  .orElseThrow(
                                                      () ->
                                                          new CustomException(
                                                              OrderErrorCode
                                                                  .INVALID_SUBSCRIBE_TYPE_CONVERSION)))
                                          .orderId(orderId)
                                          .build()));
                          log.debug(
                              "[OrderSseService] 주문 상태 변경에 의한 감소 알림 전송 성공 - 학과명: {}, 수신 제외된 타입: {}, 수신된 타입: {}",
                              booth.getDepartment().getDescription(),
                              currentSubscribeType,
                              subscribedType);
                        } catch (IOException e) {
                          log.warn(
                              "[OrderSseService] 주문 상태 변경에 의한 감소 알림 전송 실패 - 학과명: {}, 수신되었어야 할 타입: {}",
                              booth.getDepartment().getDescription(),
                              subscribedType);
                          store.remove(booth.getId(), subscribedType, emitter);
                        }
                      });
            });
  }

  @Override
  public void sendOrderDismissNotification(
      Booth booth, SseSubscribeType currentSubscribeType, Long orderId) {
    String eventName = "dismissNotification";
    List<SseEmitter> emitterList =
        store.findByBoothIdAndSubscribeType(booth.getId(), currentSubscribeType);
    emitterList.forEach(
        emitter -> {
          try {
            emitter.send(SseEmitter.event().name(eventName).data(new DismissOrderIdEvent(orderId)));
            log.debug(
                "[OrderSseService] 주문 제외 이벤트 전송 성공 - 학과명: {}, 이벤트: {}",
                booth.getDepartment().getDescription(),
                eventName);
          } catch (IOException e) {
            log.warn(
                "[OrderSseService] 주문 제외 이벤트 전송 실패 - 학과명: {}, 이벤트: {}",
                booth.getDepartment().getDescription(),
                eventName);
            store.remove(booth.getId(), currentSubscribeType, emitter);
          }
        });
  }

  @Override
  public void sendWaitingOrderEvent(Booth booth, WaitingOrderResponse waitingOrderResponse) {
    sendOrderEvent(booth, SseSubscribeType.WAITING, "waitingOrderEvent", waitingOrderResponse);
  }

  @Override
  public void sendCookingOrderEvent(Booth booth, CookingOrderResponse cookingOrderResponse) {
    sendOrderEvent(booth, SseSubscribeType.COOKING, "cookingOrderEvent", cookingOrderResponse);
  }

  @Override
  public void sendCompletedOrderEvent(Booth booth, CompletedOrderResponse completedOrderResponse) {
    sendOrderEvent(
        booth, SseSubscribeType.COMPLETED, "completedOrderEvent", completedOrderResponse);
  }

  @Override
  public void sendCanceledOrderEvent(Booth booth, CanceledOrderResponse canceledOrderResponse) {
    sendOrderEvent(booth, SseSubscribeType.CANCELED, "canceledOrderEvent", canceledOrderResponse);
  }

  @Override
  public void sendOrderItemUnitStatusEvent(
      Booth booth, OrderItemUnitStatusResponse orderItemUnitStatusResponse) {
    sendOrderEvent(
        booth, SseSubscribeType.COOKING, "orderItemUnitStatusEvent", orderItemUnitStatusResponse);
  }

  private void sendOrderEvent(
      Booth booth, SseSubscribeType sseSubscribeType, String eventName, Object data) {
    List<SseEmitter> emitterList =
        store.findByBoothIdAndSubscribeType(booth.getId(), sseSubscribeType);
    for (SseEmitter emitter : emitterList) {
      try {
        emitter.send(SseEmitter.event().name(eventName).data(data));
        log.debug(
            "[OrderSseService] 주문 이벤트 전송 성공 - 학과명: {}, 이벤트: {}",
            booth.getDepartment().getDescription(),
            eventName);
      } catch (IOException e) {
        log.warn(
            "[OrderSseService] 주문 이벤트 전송 실패 - 학과명: {}, 이벤트: {}",
            booth.getDepartment().getDescription(),
            eventName);
        store.remove(booth.getId(), sseSubscribeType, emitter);
      }
    }
  }
}
