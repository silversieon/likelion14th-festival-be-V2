/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.order.dto.request.OrderCreateRequest;
import com.skulikelion.festival.domain.order.dto.request.OrderItemUnitUpdateRequest;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;
import com.skulikelion.festival.domain.order.entity.enums.OrderCancelReason;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;
import com.skulikelion.festival.domain.order.service.OrderService;
import com.skulikelion.festival.domain.order.service.sse.OrderSseService;
import com.skulikelion.festival.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Order", description = "사용자 주문 관련 기능을 제공하는 API")
public class OrderController {

  private final OrderSseService orderSseService;
  private final OrderService orderService;

  @GetMapping(
      value = "/booths/{boothId}/orders/subscribe",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribeOrders(
      @PathVariable("boothId") Long boothId, @RequestParam("orderStatus") OrderStatus orderStatus) {
    return orderSseService.subscribeOrder(boothId, orderStatus);
  }

  @PostMapping("/booths/{boothId}/orders")
  public ResponseEntity<BaseResponse<OrderResponse>> createOrder(
      @PathVariable Long boothId, @RequestBody OrderCreateRequest request) {
    OrderResponse orderResponse = orderService.createOrder(boothId, request);
    return ResponseEntity.status(201)
        .body(BaseResponse.success(201, "주문 요청에 성공했습니다.", orderResponse));
  }

  @PreAuthorize("hasAnyRole({'BOOTH_MANAGER', 'ADMIN'})")
  @GetMapping("/booths/{boothId}/orders/waiting")
  public ResponseEntity<BaseResponse<List<WaitingOrderResponse>>> getWaitingOrders(
      @AuthenticationPrincipal String username, @PathVariable Long boothId) {
    List<WaitingOrderResponse> waitingOrders = orderService.getWaitingOrders(username, boothId);
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "대기 중인 주문 목록 조회에 성공했습니다.", waitingOrders));
  }

  @PreAuthorize("hasAnyRole({'BOOTH_MANAGER', 'ADMIN'})")
  @GetMapping("/booths/{boothId}/orders/cooking")
  public ResponseEntity<BaseResponse<List<?>>> getCookingOrders(
      @AuthenticationPrincipal String username, @PathVariable Long boothId) {
    List<CookingOrderResponse> cookingOrders = orderService.getCookingOrders(username, boothId);
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "조리 중인 테이블별 주문 목록 조회에 성공했습니다.", cookingOrders));
  }

  @PreAuthorize("hasAnyRole({'BOOTH_MANAGER', 'ADMIN'})")
  @GetMapping("/booths/{boothId}/orders/completed")
  public ResponseEntity<BaseResponse<List<?>>> getCompletedOrders(
      @AuthenticationPrincipal String username,
      @PathVariable Long boothId,
      @RequestParam(required = false) @DateTimeFormat(pattern = "M/d") LocalDate date,
      @RequestParam(required = false) String keyword) {
    List<CompletedOrderResponse> completedOrders =
        orderService.getCompletedOrders(username, boothId, date, keyword);
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "완료된 주문 목록 조회에 성공했습니다.", completedOrders));
  }

  @PreAuthorize("hasAnyRole({'BOOTH_MANAGER', 'ADMIN'})")
  @GetMapping("/booths/{boothId}/orders/canceled")
  public ResponseEntity<BaseResponse<List<?>>> getCanceledOrders(
      @AuthenticationPrincipal String username,
      @PathVariable Long boothId,
      @RequestParam(required = false) @DateTimeFormat(pattern = "M/d") LocalDate date,
      @RequestParam(required = false) String keyword) {
    List<CanceledOrderResponse> canceledOrders =
        orderService.getCanceledOrders(username, boothId, date, keyword);
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "취소된 주문 목록 조회에 성공했습니다.", canceledOrders));
  }

  @PreAuthorize("hasAnyRole({'BOOTH_MANAGER', 'ADMIN'})")
  @PatchMapping("/booths/{boothId}/orders/{orderId}/status")
  public ResponseEntity<BaseResponse<Void>> updateOrderStatus(
      @AuthenticationPrincipal String username,
      @PathVariable Long boothId,
      @PathVariable Long orderId,
      @RequestParam OrderStatus orderStatus) {
    orderService.updateOrderStatus(username, boothId, orderId, orderStatus);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "주문 상태 변경에 성공했습니다.", null));
  }

  @PreAuthorize("hasAnyRole({'BOOTH_MANAGER', 'ADMIN'})")
  @PatchMapping("/booths/{boothId}/orders/{orderId}/cancel")
  public ResponseEntity<BaseResponse<Void>> cancelOrder(
      @AuthenticationPrincipal String username,
      @PathVariable Long orderId,
      @PathVariable Long boothId,
      @RequestParam OrderCancelReason orderCancelReason) {
    orderService.cancelOrder(username, boothId, orderId, orderCancelReason);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "주문 취소에 성공했습니다.", null));
  }

  @PreAuthorize("hasAnyRole({'BOOTH_MANAGER', 'ADMIN'})")
  @PatchMapping("/booths/{boothId}/order-item-units/{orderItemUnitId}")
  public ResponseEntity<BaseResponse<Void>> updateOrderItemUnit(
      @AuthenticationPrincipal String username,
      @PathVariable Long boothId,
      @PathVariable Long orderItemUnitId,
      @RequestBody OrderItemUnitUpdateRequest request) {
    orderService.updateServedStatus(username, boothId, orderItemUnitId, request);
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "주문 상세 개별 상태 변경에 성공했습니다.", null));
  }
}
