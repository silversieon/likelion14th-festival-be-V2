/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.controller;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscriber;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * 멋쟁이사자처럼 서경대학교 축제 페이지 주문 SSE Controller 입니다.
 *
 * @since 2026.08.30
 * @see OrderSseSubscriber
 * @author Keum Si Eon
 * @version latest: 1
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class OrderSseController {

  private final OrderSseSubscriber subscriber;

  @Operation(
      summary = "[ 부스 관리자 | 토큰 O | 주문 관리 탭별 구독 ]",
      description =
          """
                            **Parameters**  \n
                            orderSseSubscribeType: 구독 타입(관리 탭 명칭)
                            \n
                            **Returns** \n
                            EVENT NAME: connect \n
                            EVENT DATA: connected order subscribe
                            """)
  @PreAuthorize("hasRole('BOOTH_MANAGER')")
  @GetMapping(value = "/orders/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribeOrders(
      @AuthenticationPrincipal String departmentName,
      @RequestParam("orderSseSubscribeType") OrderSseSubscribeType orderSseSubscribeType) {
    return subscriber.subscribeOrderStatus(departmentName, orderSseSubscribeType);
  }
}
