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
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.domain.booth.repository.BoothRepository;
import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.domain.manager.exception.ManagerErrorCode;
import com.skulikelion.festival.domain.manager.repository.ManagerRepository;
import com.skulikelion.festival.domain.order.dto.event.OrderStatusNotificationEvent;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderItemUnitStatusResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;
import com.skulikelion.festival.domain.order.enums.SseSubscribeType;
import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.domain.order.repository.sse.OrderSseEmitterRepository;
import com.skulikelion.festival.global.enums.Department;
import com.skulikelion.festival.global.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderSseServiceImpl implements OrderSseService {

  private final OrderSseEmitterRepository orderSseEmitterRepository;
  private final BoothRepository boothRepository;
  private final ManagerRepository managerRepository;

  @Override
  public SseEmitter subscribeOrder(String departmentName, SseSubscribeType sseSubscribeType) {
    Booth booth = validateBoothExists(departmentName);
    validateBoothManager(departmentName, booth);
    Long boothId = booth.getId();
    SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);

    emitter.onCompletion(
        () -> orderSseEmitterRepository.remove(boothId, sseSubscribeType, emitter));
    emitter.onTimeout(() -> orderSseEmitterRepository.remove(boothId, sseSubscribeType, emitter));
    emitter.onError(e -> orderSseEmitterRepository.remove(boothId, sseSubscribeType, emitter));

    orderSseEmitterRepository.save(boothId, sseSubscribeType, emitter);

    try {
      emitter.send(SseEmitter.event().name("connect").data("connected order subscribe"));
    } catch (IOException e) {
      orderSseEmitterRepository.remove(boothId, sseSubscribeType, emitter);
    }

    return emitter;
  }

  @Override
  public void sendOrderEventNotification(Booth booth, SseSubscribeType currentSubscribeType) {
    Map<SseSubscribeType, List<SseEmitter>> statusMap =
        orderSseEmitterRepository.findByBoothId(booth.getId());

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
                                  .name("orderNotification")
                                  .data(
                                      new OrderStatusNotificationEvent(
                                          currentSubscribeType
                                              .toOrderStatus()
                                              .orElseThrow(
                                                  () ->
                                                      new CustomException(
                                                          OrderErrorCode
                                                              .INVALID_SUBSCRIBE_TYPE_CONVERSION)))));
                        } catch (IOException e) {
                          orderSseEmitterRepository.remove(booth.getId(), subscribedType, emitter);
                        }
                      });
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
        orderSseEmitterRepository.findByBoothIdAndSubscribeType(booth.getId(), sseSubscribeType);
    for (SseEmitter emitter : emitterList) {
      try {
        emitter.send(SseEmitter.event().name(eventName).data(data));
      } catch (IOException e) {
        orderSseEmitterRepository.remove(booth.getId(), sseSubscribeType, emitter);
      }
    }
  }

  private Booth validateBoothExists(String departmentName) {
    return boothRepository
        .findByDepartment(Department.valueOf(departmentName))
        .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));
  }

  private void validateBoothManager(String departmentName, Booth booth) {
    Department department = Department.valueOf(departmentName);
    Manager currentManager =
        managerRepository
            .findByDepartment(department)
            .orElseThrow(() -> new CustomException(ManagerErrorCode.MANAGER_NOT_FOUND));
    if (!booth.getDepartment().equals(currentManager.getDepartment())
        && currentManager.getRole() != Role.ADMIN) {
      throw new CustomException(OrderErrorCode.BOOTH_ACCESS_DENIED);
    }
  }
}
