/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.local;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.order.dto.response.*;
import com.skulikelion.festival.domain.order.sse.OrderSseEventType;
import com.skulikelion.festival.domain.order.sse.OrderSseNotifier;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;
import com.skulikelion.festival.domain.order.sse.dto.DismissOrderIdNotification;
import com.skulikelion.festival.domain.order.sse.dto.OrderCountNotification;
import com.skulikelion.festival.domain.order.sse.store.LocalOrderSseEmitterStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class LocalOrderSseNotifier implements OrderSseNotifier {

  private final LocalOrderSseEmitterStore store;

  @Override
  public void sendOrderIncrementNotification(
      Booth booth, OrderSseSubscribeType currentSubscribeType, Long orderId) {
    sendOrderCountNotification(
        booth,
        currentSubscribeType,
        orderId,
        OrderSseEventType.ORDER_INCREMENT_NOTIFICATION.getEventName());
  }

  @Override
  public void sendOrderDecrementNotification(
      Booth booth, OrderSseSubscribeType currentSubscribeType, Long orderId) {
    sendOrderCountNotification(
        booth,
        currentSubscribeType,
        orderId,
        OrderSseEventType.ORDER_DECREMENT_NOTIFICATION.getEventName());
  }

  @Override
  public void sendOrderDismissNotification(
      Booth booth, OrderSseSubscribeType currentSubscribeType, Long orderId) {
    sendOrderEventNotification(
        booth,
        currentSubscribeType,
        OrderSseEventType.DISMISS_NOTIFICATION.getEventName(),
        DismissOrderIdNotification.of(orderId));
  }

  @Override
  public void sendWaitingOrderEvent(Booth booth, WaitingOrderResponse waitingOrderResponse) {
    sendOrderEventNotification(
        booth,
        OrderSseSubscribeType.WAITING,
        OrderSseEventType.WAITING_ORDER_EVENT.getEventName(),
        waitingOrderResponse);
  }

  @Override
  public void sendCookingOrderEvent(Booth booth, CookingOrderResponse cookingOrderResponse) {
    sendOrderEventNotification(
        booth,
        OrderSseSubscribeType.COOKING,
        OrderSseEventType.COOKING_ORDER_EVENT.getEventName(),
        cookingOrderResponse);
  }

  @Override
  public void sendCompletedOrderEvent(Booth booth, CompletedOrderResponse completedOrderResponse) {
    sendOrderEventNotification(
        booth,
        OrderSseSubscribeType.COMPLETED,
        OrderSseEventType.COMPLETED_ORDER_EVENT.getEventName(),
        completedOrderResponse);
  }

  @Override
  public void sendCanceledOrderEvent(Booth booth, CanceledOrderResponse canceledOrderResponse) {
    sendOrderEventNotification(
        booth,
        OrderSseSubscribeType.CANCELED,
        OrderSseEventType.CANCELED_ORDER_EVENT.getEventName(),
        canceledOrderResponse);
  }

  @Override
  public void sendOrderItemUnitStatusEvent(
      Booth booth, OrderItemUnitStatusResponse orderItemUnitStatusResponse) {
    sendOrderEventNotification(
        booth,
        OrderSseSubscribeType.COOKING,
        OrderSseEventType.ORDER_ITEM_UNIT_STATUS_EVENT.getEventName(),
        orderItemUnitStatusResponse);
  }

  private void sendOrderEventNotification(
      Booth booth, OrderSseSubscribeType orderSseSubscribeType, String eventName, Object data) {
    List<SseEmitter> emitterList =
        store.findByBoothIdAndSubscribeType(booth.getId(), orderSseSubscribeType);
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
        store.remove(booth.getId(), orderSseSubscribeType, emitter);
      }
    }
  }

  private void sendOrderCountNotification(
      Booth booth, OrderSseSubscribeType excludedType, Long orderId, String eventName) {
    Map<OrderSseSubscribeType, List<SseEmitter>> statusMap = store.findByBoothId(booth.getId());
    OrderCountNotification orderCountNotification =
        OrderCountNotification.of(excludedType, orderId);

    statusMap.entrySet().stream()
        .filter(entry -> entry.getKey() != excludedType)
        .forEach(
            entry -> {
              OrderSseSubscribeType subscribedType = entry.getKey();
              entry
                  .getValue()
                  .forEach(
                      emitter -> {
                        try {
                          emitter.send(
                              SseEmitter.event().name(eventName).data(orderCountNotification));
                          log.debug(
                              "[OrderSseService] 주문 상태 변경에 의한 감소 알림 전송 성공 - 학과명: {}, 이벤트명: {}, 수신 제외된 타입: {}, 수신된 타입: {}",
                              booth.getDepartment().getDescription(),
                              eventName,
                              excludedType,
                              subscribedType);
                        } catch (IOException e) {
                          log.warn(
                              "[OrderSseService] 주문 상태 변경에 의한 감소 알림 전송 실패 - 학과명: {}, 이벤트명: {}, 수신되었어야 할 타입: {}",
                              booth.getDepartment().getDescription(),
                              eventName,
                              subscribedType);
                          store.remove(booth.getId(), subscribedType, emitter);
                        }
                      });
            });
  }
}
