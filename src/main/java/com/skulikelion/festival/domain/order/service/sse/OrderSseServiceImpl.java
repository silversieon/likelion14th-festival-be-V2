/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.sse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.order.dto.event.OrderStatusNotificationEvent;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderItemUnitResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;
import com.skulikelion.festival.domain.order.repository.sse.OrderSseEmitterRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderSseServiceImpl implements OrderSseService {

  private final OrderSseEmitterRepository orderSseEmitterRepository;

  @Override
  public SseEmitter subscribeOrder(Long boothId, OrderStatus orderStatus) {
    SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);

    emitter.onCompletion(() -> orderSseEmitterRepository.remove(boothId, orderStatus, emitter));
    emitter.onTimeout(() -> orderSseEmitterRepository.remove(boothId, orderStatus, emitter));
    emitter.onError(e -> orderSseEmitterRepository.remove(boothId, orderStatus, emitter));

    orderSseEmitterRepository.save(boothId, orderStatus, emitter);

    try {
      emitter.send(
          SseEmitter.event().name("order subscribe connect").data("connected order subscribe"));
    } catch (IOException e) {
      orderSseEmitterRepository.remove(boothId, orderStatus, emitter);
    }

    return emitter;
  }

  @Override
  public void sendOrderEventNotification(Booth booth, OrderStatus currentStatus) {
    Map<OrderStatus, List<SseEmitter>> statusMap =
        orderSseEmitterRepository.findByBoothId(booth.getId());

    statusMap.entrySet().stream()
        .filter(entry -> entry.getKey() != currentStatus)
        .forEach(
            entry -> {
              OrderStatus subscribedStatus = entry.getKey();
              entry
                  .getValue()
                  .forEach(
                      emitter -> {
                        try {
                          emitter.send(
                              SseEmitter.event()
                                  .name("orderNotification")
                                  .data(new OrderStatusNotificationEvent(currentStatus)));
                        } catch (IOException e) {
                          orderSseEmitterRepository.remove(
                              booth.getId(), subscribedStatus, emitter);
                        }
                      });
            });
  }

  @Override
  public void sendWaitingOrderEvent(Booth booth, WaitingOrderResponse waitingOrderResponse) {
    sendOrderEvent(booth, OrderStatus.WAITING, "waitingOrderEvent", waitingOrderResponse);
  }

  @Override
  public void sendCookingOrderEvent(Booth booth, CookingOrderResponse cookingOrderResponse) {
    sendOrderEvent(booth, OrderStatus.COOKING, "cookingOrderEvent", cookingOrderResponse);
  }

  @Override
  public void sendCompletedOrderEvent(Booth booth, CompletedOrderResponse completedOrderResponse) {
    sendOrderEvent(booth, OrderStatus.COMPLETED, "completedOrderEvent", completedOrderResponse);
  }

  @Override
  public void sendCanceledOrderEvent(Booth booth, CanceledOrderResponse canceledOrderResponse) {
    sendOrderEvent(booth, OrderStatus.CANCELED, "canceledOrderEvent", canceledOrderResponse);
  }

  @Override
  public void sendCookingOrderItemUnitEvent(
      Booth booth, CookingOrderItemUnitResponse cookingOrderItemUnitResponse) {
    sendOrderEvent(
        booth, OrderStatus.COOKING, "cookingOrderItemUnitEvent", cookingOrderItemUnitResponse);
  }

  private void sendOrderEvent(Booth booth, OrderStatus orderStatus, String eventName, Object data) {
    List<SseEmitter> emitterList =
        orderSseEmitterRepository.findByBoothIdAndOrderStatus(booth.getId(), orderStatus);
    for (SseEmitter emitter : emitterList) {
      try {
        emitter.send(SseEmitter.event().name(eventName).data(data));
      } catch (IOException e) {
        orderSseEmitterRepository.remove(booth.getId(), orderStatus, emitter);
      }
    }
  }
}
