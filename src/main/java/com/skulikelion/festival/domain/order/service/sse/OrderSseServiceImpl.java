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
        () -> {
          log.info(
              "[OrderSseService] SSE 연결 종료 - 학과명: {}, 구독 타입: {}", departmentName, sseSubscribeType);
          orderSseEmitterRepository.remove(boothId, sseSubscribeType, emitter);
        });
    emitter.onTimeout(
        () -> {
          log.info(
              "[OrderSseService] SSE 타임아웃 - 학과명: {}, 구독 타입: {}", departmentName, sseSubscribeType);
          orderSseEmitterRepository.remove(boothId, sseSubscribeType, emitter);
        });
    emitter.onError(
        e -> {
          log.warn(
              "[OrderSseService] SSE 에러 - 학과명: {}, 구독 타입: {}", departmentName, sseSubscribeType);
          orderSseEmitterRepository.remove(boothId, sseSubscribeType, emitter);
        });

    orderSseEmitterRepository.save(boothId, sseSubscribeType, emitter);

    try {
      emitter.send(SseEmitter.event().name("connect").data("connected order subscribe"));
      log.debug(
          "[OrderSseService] SSE 구독 성공 - 학과명: {}, 구독 타입: {}", departmentName, sseSubscribeType);
    } catch (IOException e) {
      log.warn(
          "[OrderSseService] SSE 초기 연결 이벤트 전송 실패 - 학과명: {}, 구독 타입: {}",
          departmentName,
          sseSubscribeType);
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
                          log.debug(
                              "[OrderSseService] 주문 상태 변경 알림 전송 성공 - 학과명: {}, 변경된 상태: {}, 수신 구독 타입: {}",
                              booth.getDepartment().getDescription(),
                              currentSubscribeType,
                              subscribedType);
                        } catch (IOException e) {
                          log.warn(
                              "[OrderSseService] 주문 상태 변경 알림 전송 실패 - 학과명: {}, 수신 구독 타입: {}",
                              booth.getDepartment().getDescription(),
                              subscribedType);
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
        log.debug(
            "[OrderSseService] 주문 이벤트 전송 성공 - 학과명: {}, 이벤트: {}",
            booth.getDepartment().getDescription(),
            eventName);
      } catch (IOException e) {
        log.warn(
            "[OrderSseService] 주문 이벤트 전송 실패 - 학과명: {}, 이벤트: {}",
            booth.getDepartment().getDescription(),
            eventName);
        orderSseEmitterRepository.remove(booth.getId(), sseSubscribeType, emitter);
      }
    }
  }

  private Booth validateBoothExists(String departmentName) {
    return boothRepository
        .findByDepartment(Department.valueOf(departmentName))
        .orElseThrow(
            () -> {
              log.warn("[OrderSseService] 해당 학과의 부스를 찾을 수 없습니다 - 학과명: {}", departmentName);
              return new CustomException(BoothErrorCode.BOOTH_NOT_FOUND);
            });
  }

  private void validateBoothManager(String departmentName, Booth booth) {
    Department department = Department.valueOf(departmentName);
    Manager currentManager =
        managerRepository
            .findByDepartment(department)
            .orElseThrow(
                () -> {
                  log.warn("[OrderSseService] 해당 학과의 매니저를 찾을 수 없습니다 - 학과명: {}", departmentName);
                  return new CustomException(ManagerErrorCode.MANAGER_NOT_FOUND);
                });
    if (!booth.getDepartment().equals(currentManager.getDepartment())
        && currentManager.getRole() != Role.ADMIN) {
      log.warn(
          "[OrderSseService] 부스 접근 권한 없음 - 학과명: {}, 매니저 역할: {}",
          departmentName,
          currentManager.getRole());
      throw new CustomException(OrderErrorCode.BOOTH_ACCESS_DENIED);
    }
  }
}
