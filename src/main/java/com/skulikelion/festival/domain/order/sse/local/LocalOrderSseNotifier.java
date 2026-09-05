/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.local;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.order.event.payload.CanceledOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.CompletedOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.CookingOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.DismissOrderPayload;
import com.skulikelion.festival.domain.order.event.payload.OrderItemUnitStatusPayload;
import com.skulikelion.festival.domain.order.event.payload.WaitingOrderPayload;
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
      Long boothId, OrderSseSubscribeType currentSubscribeType, Long orderId) {
    sendOrderCountNotification(
        boothId,
        currentSubscribeType,
        orderId,
        OrderSseEventType.ORDER_INCREMENT_NOTIFICATION.getEventName());
  }

  @Override
  public void sendOrderDecrementNotification(
      Long boothId, OrderSseSubscribeType currentSubscribeType, Long orderId) {
    sendOrderCountNotification(
        boothId,
        currentSubscribeType,
        orderId,
        OrderSseEventType.ORDER_DECREMENT_NOTIFICATION.getEventName());
  }

  @Override
  public void sendOrderDismissNotification(DismissOrderPayload payload) {
    sendOrderEventNotification(
        payload.boothId(),
        payload.currentOrderStatus().toSseSubscribeType(),
        OrderSseEventType.DISMISS_NOTIFICATION.getEventName(),
        DismissOrderIdNotification.of(payload.orderId()));
  }

  @Override
  public void sendWaitingOrderEvent(WaitingOrderPayload payload) {
    sendOrderEventNotification(
        payload.boothId(),
        OrderSseSubscribeType.WAITING,
        OrderSseEventType.WAITING_ORDER_EVENT.getEventName(),
        payload.waitingOrderResponse());
  }

  @Override
  public void sendCookingOrderEvent(CookingOrderPayload payload) {
    sendOrderEventNotification(
        payload.boothId(),
        OrderSseSubscribeType.COOKING,
        OrderSseEventType.COOKING_ORDER_EVENT.getEventName(),
        payload.cookingOrderResponse());
  }

  @Override
  public void sendCompletedOrderEvent(CompletedOrderPayload payload) {
    sendOrderEventNotification(
        payload.boothId(),
        OrderSseSubscribeType.COMPLETED,
        OrderSseEventType.COMPLETED_ORDER_EVENT.getEventName(),
        payload.completedOrderResponse());
  }

  @Override
  public void sendCanceledOrderEvent(CanceledOrderPayload payload) {
    sendOrderEventNotification(
        payload.boothId(),
        OrderSseSubscribeType.CANCELED,
        OrderSseEventType.CANCELED_ORDER_EVENT.getEventName(),
        payload.canceledOrderResponse());
  }

  @Override
  public void sendOrderItemUnitStatusEvent(OrderItemUnitStatusPayload payload) {
    sendOrderEventNotification(
        payload.boothId(),
        OrderSseSubscribeType.COOKING,
        OrderSseEventType.ORDER_ITEM_UNIT_STATUS_EVENT.getEventName(),
        payload.orderItemUnitStatusResponse());
  }

  private void sendOrderEventNotification(
      Long boothId, OrderSseSubscribeType orderSseSubscribeType, String eventName, Object data) {
    List<SseEmitter> emitterList =
        store.findByBoothIdAndSubscribeType(boothId, orderSseSubscribeType);
    log.info("[OrderSseNotifier] sending order event notification!");
    for (SseEmitter emitter : emitterList) {
      try {
        emitter.send(SseEmitter.event().name(eventName).data(data));
        log.debug("[OrderSseService] 주문 이벤트 전송 성공 - 부스 식별자: {}, 이벤트: {}", boothId, eventName);
      } catch (IOException e) {
        log.warn("[OrderSseService] 주문 이벤트 전송 실패 - 부스 식별자: {}, 이벤트: {}", boothId, eventName);
        store.remove(boothId, orderSseSubscribeType, emitter);
      }
    }
  }

  private void sendOrderCountNotification(
      Long boothId, OrderSseSubscribeType excludedType, Long orderId, String eventName) {
    Map<OrderSseSubscribeType, List<SseEmitter>> statusMap = store.findByBoothId(boothId);
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
                              "[OrderSseService] 주문 상태 변경에 의한 감소 알림 전송 성공 - 부스 식별자: {}, 이벤트명: {}, 수신 제외된 타입: {}, 수신된 타입: {}",
                              boothId,
                              eventName,
                              excludedType,
                              subscribedType);
                        } catch (IOException e) {
                          log.warn(
                              "[OrderSseService] 주문 상태 변경에 의한 감소 알림 전송 실패 - 부스 식별자: {}, 이벤트명: {}, 수신되었어야 할 타입: {}",
                              boothId,
                              eventName,
                              subscribedType);
                          store.remove(boothId, subscribedType, emitter);
                        }
                      });
            });
  }
}
