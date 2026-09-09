/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.local;

import java.io.IOException;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.service.booth.BoothService;
import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.domain.manager.service.ManagerService;
import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscriber;
import com.skulikelion.festival.domain.order.sse.store.OrderSseEmitterRegistry;
import com.skulikelion.festival.global.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class LocalOrderSseSubscriber implements OrderSseSubscriber {

  private final BoothService boothService;
  private final ManagerService managerService;
  private final OrderSseEmitterRegistry registry;

  @Override
  public SseEmitter subscribeOrderStatus(
      String departmentName, OrderSseSubscribeType subscribeType) {
    Booth booth = boothService.getRequiredBooth(departmentName);
    Manager manager = managerService.getRequiredManager(departmentName);
    validateBoothManagerAuthority(booth, manager);

    Long boothId = booth.getId();
    SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);

    registry.register(boothId, subscribeType, emitter);
    registerUnsubscribeCallback(emitter, boothId, subscribeType);
    connectCheck(emitter, boothId, subscribeType);
    return emitter;
  }

  private void registerUnsubscribeCallback(
      SseEmitter emitter, Long boothId, OrderSseSubscribeType subscribeType) {
    emitter.onCompletion(
        () -> {
          log.debug("[OrderSseService] SSE 연결 종료 - 부스 식별자: {}, 구독 타입: {}", boothId, subscribeType);
          registry.remove(boothId, subscribeType, emitter);
        });
    emitter.onTimeout(
        () -> {
          log.debug("[OrderSseService] SSE 타임아웃 - 부스 식별자: {}, 구독 타입: {}", boothId, subscribeType);
          registry.remove(boothId, subscribeType, emitter);
          emitter.complete();
        });
    emitter.onError(
        e -> {
          log.warn("[OrderSseService] SSE 에러 - 부스 식별자: {}, 구독 타입: {}", boothId, subscribeType);
          registry.remove(boothId, subscribeType, emitter);
          emitter.complete();
        });
  }

  private void connectCheck(SseEmitter emitter, Long boothId, OrderSseSubscribeType subscribeType) {
    try {
      emitter.send(SseEmitter.event().name("connect").data("connected order subscribe"));
    } catch (IOException e) {
      registry.remove(boothId, subscribeType, emitter);
    }
  }

  private void validateBoothManagerAuthority(Booth booth, Manager manager) {
    if (!booth.getDepartment().equals(manager.getDepartment()) && manager.getRole() != Role.ADMIN) {
      log.warn(
          "[OrderSseService] 부스 접근 권한 없음 - 학과명: {}, 매니저 역할: {}",
          booth.getDepartment(),
          manager.getRole());
      throw new CustomException(OrderErrorCode.BOOTH_ACCESS_DENIED);
    }
  }
}
