/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.controller;

import java.awt.*;
import java.time.LocalDate;
import java.util.List;

import com.skulikelion.festival.domain.order.dto.request.CookingOrderCursor;
import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.order.dto.request.OrderCreateRequest;
import com.skulikelion.festival.domain.order.dto.request.OrderItemUnitUpdateRequest;
import com.skulikelion.festival.domain.order.dto.request.WaitingOrderCursor;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderResponse;
import com.skulikelion.festival.domain.order.dto.response.SalesResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;
import com.skulikelion.festival.domain.order.entity.enums.OrderCancelReason;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;
import com.skulikelion.festival.domain.order.enums.SseSubscribeType;
import com.skulikelion.festival.domain.order.service.OrderService;
import com.skulikelion.festival.domain.order.service.sse.OrderSseService;
import com.skulikelion.festival.global.common.BaseResponse;
import com.skulikelion.festival.global.common.pagenation.CursorCodec;
import com.skulikelion.festival.global.common.pagenation.CursorPageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 멋쟁이사자처럼 서경대학교 축제 페이지 주문 관련 Controller 입니다.
 *
 * @since 2026.04.29
 * @see OrderService
 * @author Keum Si Eon
 * @version latest: 1
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Order", description = "사용자 주문 관련 기능을 제공하는 API")
public class OrderController {

  private final OrderSseService orderSseService;
  private final OrderService orderService;
  private final CursorCodec cursorCodec;

  @Operation(
      summary = "[ 부스 관리자 | 토큰 O | 주문 관리 탭별 구독 ]",
      description =
          """
                  **Parameters**  \n
                  sseSubscribeType: 구독 타입(관리 탭 명칭)
                  \n
                  **Returns** \n
                  EVENT NAME: connect \n
                  EVENT DATA: connected order subscribe
                  """)
  @PreAuthorize("hasRole('BOOTH_MANAGER')")
  @GetMapping(value = "/orders/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribeOrders(
      @AuthenticationPrincipal String departmentName,
      @RequestParam("sseSubscribeType") SseSubscribeType sseSubscribeType) {
    return orderSseService.subscribeOrder(departmentName, sseSubscribeType);
  }

  @Operation(
      summary = "[ 사용자 | 토큰 X | 주문 생성 요청 ]",
      description =
          """
                  **Parameters**  \n
                  Idempotency-Key: 멱등성 키 \n
                  tableNumber: 테이블 번호 \n
                  numOfPeople: 인원 수 \n
                  customerName: 주문자 이름 \n
                  customerPhoneNumber: 주문자 전화번호 \n
                  totalOrderPrice: 총 주문 금액 \n
                  language: 언어 타입(KO, EN, ZH) \n
                  orderItems: 주문한 메뉴 목록 \n
                  {
                  boothMenuId: 부스 메뉴 식별자 \n
                  quantity: 메뉴 개수 \n
                  menuPrice: 메뉴 가격 \n
                  totalOrderItemPrice: 주문 상세 총 가격
                  } \n
                  \n
                  **Returns** \n
                  orderId: 주문 식별자 \n
                  customerName: 주문자 이름 \n
                  customerPhoneNumber: 주문자 전화번호 \n
                  orderTime: 주문 시각 \n
                  totalOrderPrice: 총 주문 금액 \n
                  bankName: 은행 이름 \n
                  accountName: 예금주 \n
                  accountNumber: 계좌번호 \n
                  orderItems: 주문한 메뉴 목록 \n
                  {
                  orderItemId: 주문 상세 식별자 \n
                  orderId: 주문 식별자 \n
                  menuName: 메뉴명 \n
                  quantity: 메뉴 개수 \n
                  menuPrice: 메뉴 개별 가격 \n
                  totalOrderItemPrice: 주문 상세 총 가격
                  } \n
                  \n
                  EVENT NAME: waitingOrderEvent \n
                  EVENT DATA: 대기 중 주문 응답 (json)
                  """)
  @PostMapping("/booths/{boothId}/orders")
  public ResponseEntity<BaseResponse<OrderResponse>> createOrder(
      @PathVariable Long boothId,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody OrderCreateRequest request) {
    OrderResponse orderResponse = orderService.createOrder(boothId, idempotencyKey, request);
    return ResponseEntity.status(201)
        .body(BaseResponse.success(201, "주문 요청에 성공했습니다.", orderResponse));
  }

  @Operation(
      summary = "[ 부스 관리자 | 토큰 O | 대기 중 주문 목록 조회 ]",
      description =
          """
                  **Returns** \n
                  orderId: 주문 식별자 \n
                  tableNumber: 테이블 번호 \n
                  numOfPeople: 인원 수 \n
                  customerName: 주문자 이름 \n
                  customerPhoneNumber: 주문자 전화번호 \n
                  orderTime: 주문 시각 \n
                  totalOrderPrice: 총 주문 금액 \n
                  orderItems: 주문한 메뉴 목록 \n
                  {
                  orderItemId: 주문 상세 식별자 \n
                  orderId: 주문 식별자 \n
                  menuName: 메뉴명 \n
                  quantity: 메뉴 개수 \n
                  totalOrderItemPrice: 주문 상세 총 가격
                  }
                  """)
  @PreAuthorize("hasRole('BOOTH_MANAGER')")
  @GetMapping("/orders/waiting")
  public ResponseEntity<BaseResponse<CursorPageResponse<WaitingOrderResponse>>> getWaitingOrders(
      @AuthenticationPrincipal String departmentName,
      @RequestParam(required = false) String encodedCursor,
      @RequestParam(defaultValue = "20") Integer size) {
    WaitingOrderCursor cursor = cursorCodec.decode(encodedCursor, WaitingOrderCursor.class);
    CursorPageResponse<WaitingOrderResponse> waitingOrders =
        orderService.getWaitingOrders(departmentName, cursor, size);
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "대기 중인 주문 목록 조회에 성공했습니다.", waitingOrders));
  }

  @Operation(
      summary = "[ 부스 관리자 | 토큰 O | 조리 중 주문 목록 조회 ]",
      description =
          """
                  **Returns** \n
                  orderId: 주문 식별자 \n
                  tableNumber: 테이블 번호 \n
                  numOfPeople: 인원 수 \n
                  customerName: 주문자 이름 \n
                  customerPhoneNumber: 주문자 전화번호 \n
                  orderTime: 주문 시각 \n
                  totalOrderPrice: 총 주문 금액 \n
                  orderItemUnits: 주문한 메뉴 상세 개별 목록 \n
                  {
                  orderItemUnitId: 주문 상세 개별 식별자 \n
                  orderId: 주문 식별자 \n
                  menuName: 메뉴명 \n
                  menuPrice: 메뉴 개별 가격 \n
                  isServed: 완료(또는 서빙) 여부
                 }
                 """)
  @PreAuthorize("hasRole('BOOTH_MANAGER')")
  @GetMapping("/orders/cooking")
  public ResponseEntity<BaseResponse<CursorPageResponse<CookingOrderResponse>>> getCookingOrders(
      @AuthenticationPrincipal String departmentName,
      @RequestParam(required = false) String encodedCursor,
      @RequestParam(defaultValue = "20") Integer size) {
    CookingOrderCursor cursor = cursorCodec.decode(encodedCursor, CookingOrderCursor.class);
    CursorPageResponse<CookingOrderResponse> cookingOrders = orderService.getCookingOrders(departmentName, cursor, size);
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "조리 중인 테이블별 주문 목록 조회에 성공했습니다.", cookingOrders));
  }

  @Operation(
      summary = "[ 부스 관리자 | 토큰 O | 완료된 주문 목록 조회 ]",
      description =
          """
                  **Parameters** \n
                  date: 조회할 날짜 (2026-05-01 형태) \n
                  keyword: 검색어 \n
                  \n
                  **Returns** \n
                  orderId: 주문 식별자 \n
                  tableNumber: 테이블 번호 \n
                  numOfPeople: 인원 수 \n
                  customerName: 주문자 이름 \n
                  customerPhoneNumber: 주문자 전화번호 \n
                  orderTime: 주문 시각 \n
                  totalOrderPrice: 총 주문 금액 \n
                  orderDate: 주문 날짜 \n
                  orderTime: 주문 시각 \n
                  completeTime: 완료 시각 \n
                  orderItems: 주문한 메뉴 상세 목록 \n
                  {
                  orderItemId: 주문 상세 개별 식별자 \n
                  orderId: 주문 식별자 \n
                  menuName: 메뉴명 \n
                  quantity: 메뉴 개수 \n
                  totalOrderItemPrice: 주문 상세 총 가격
                 }
                 """)
  @PreAuthorize("hasRole('BOOTH_MANAGER')")
  @GetMapping("/orders/completed")
  public ResponseEntity<BaseResponse<List<CompletedOrderResponse>>> getCompletedOrders(
      @AuthenticationPrincipal String departmentName,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) String keyword) {
    List<CompletedOrderResponse> completedOrders =
        orderService.getCompletedOrders(departmentName, date, keyword);
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "완료된 주문 목록 조회에 성공했습니다.", completedOrders));
  }

  @Operation(
      summary = "[ 부스 관리자 | 토큰 O | 취소된 주문 목록 조회 ]",
      description =
          """
                  **Parameters** \n
                  date: 조회할 날짜 (2026-05-01 형태) \n
                  keyword: 검색어 \n
                  \n
                  **Returns** \n
                  orderId: 주문 식별자 \n
                  tableNumber: 테이블 번호 \n
                  numOfPeople: 인원 수 \n
                  customerName: 주문자 이름 \n
                  customerPhoneNumber: 주문자 전화번호 \n
                  orderTime: 주문 시각 \n
                  totalOrderPrice: 총 주문 금액 \n
                  orderDate: 주문 날짜 \n
                  orderTime: 주문 시각 \n
                  cancelTime: 취소 시각 \n
                  orderCancelReason: 주문 취소 사유 \n
                  orderItems: 주문한 메뉴 상세 목록 \n
                  {
                  orderItemId: 주문 상세 개별 식별자 \n
                  orderId: 주문 식별자 \n
                  menuName: 메뉴명 \n
                  quantity: 메뉴 개수 \n
                  totalOrderItemPrice: 주문 상세 총 가격
                 }
                 """)
  @PreAuthorize("hasRole('BOOTH_MANAGER')")
  @GetMapping("/orders/canceled")
  public ResponseEntity<BaseResponse<List<CanceledOrderResponse>>> getCanceledOrders(
      @AuthenticationPrincipal String departmentName,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) String keyword) {
    List<CanceledOrderResponse> canceledOrders =
        orderService.getCanceledOrders(departmentName, date, keyword);
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "취소된 주문 목록 조회에 성공했습니다.", canceledOrders));
  }

  @Operation(
      summary = "[ 부스 관리자 | 토큰 O | 날짜별 매출 조회 ]",
      description =
          """
          **Parameters** \n
          date: 조회할 날짜 (2026-05-01 형태) \n
          \n
          **Returns** \n
          sales: 매출 \n
          """)
  @PreAuthorize("hasRole('BOOTH_MANAGER')")
  @GetMapping("/orders/sales")
  public ResponseEntity<BaseResponse<SalesResponse>> getSales(
      @AuthenticationPrincipal String departmentName,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date) {
    SalesResponse response = orderService.getSales(departmentName, date);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "매출 조회에 성공했습니다.", response));
  }

  @Operation(
      summary = "[ 부스 관리자 | 토큰 O | 주문 상태 변경 ]",
      description =
          """
                  **Parameters**  \n
                  orderId: 주문 식별자 \n
                  orderStatus: 주문 상태 (대기, 취소로 전환 X) \n
                  \n
                  **Returns** \n
                  EVENT1 { \n
                  EVENT NAME: orderNotification \n
                  EVENT DATA: 주문 상태 (json) \n
                  } \n
                  \n
                  EVENT2 { \n
                  EVENT NAME: cookingOrderEvent 또는 completedOrderEvent \n
                  EVENT DATA: 조리 중, 완료된 주문 응답 (json) \n
                  }
                  """)
  @PreAuthorize("hasRole('BOOTH_MANAGER')")
  @PatchMapping("/orders/{orderId}/status")
  public ResponseEntity<BaseResponse<Void>> updateOrderStatus(
      @AuthenticationPrincipal String departmentName,
      @PathVariable Long orderId,
      @RequestParam OrderStatus orderStatus) {
    orderService.updateOrderStatus(departmentName, orderId, orderStatus);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "주문 상태 변경에 성공했습니다.", null));
  }

  @Operation(
      summary = "[ 부스 관리자 | 토큰 O | 주문 취소 ]",
      description =
          """
                  **Parameters**  \n
                  orderId: 주문 식별자 \n
                  orderCancelReason: 주문 상태 (대기, 취소로 전환 X) \n
                  \n
                  **Returns** \n
                  EVENT NAME: canceledOrderEvent \n
                  EVENT DATA: 취소된 주문 응답 (json)
                  """)
  @PreAuthorize("hasRole('BOOTH_MANAGER')")
  @PatchMapping("/orders/{orderId}/cancel")
  public ResponseEntity<BaseResponse<Void>> cancelOrder(
      @AuthenticationPrincipal String departmentName,
      @PathVariable Long orderId,
      @RequestParam OrderCancelReason orderCancelReason) {
    orderService.cancelOrder(departmentName, orderId, orderCancelReason);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "주문 취소에 성공했습니다.", null));
  }

  @Operation(
      summary = "[ 부스 관리자 | 토큰 O | 주문 상세 개별 서빙 여부 변경 ]",
      description =
          """
                  **Parameters**  \n
                  orderItemUnitId: 주문 상세 개별 식별자 \n
                  isServed: 서빙 여부 \n
                  \n
                  **Returns** \n
                  EVENT NAME: orderItemUnitStatusEvent \n
                  EVENT DATA: 주문 상세 개별 서빙 여부 (json)
                  """)
  @PreAuthorize("hasRole('BOOTH_MANAGER')")
  @PatchMapping("/order-item-units/{orderItemUnitId}")
  public ResponseEntity<BaseResponse<Void>> updateOrderItemUnit(
      @AuthenticationPrincipal String departmentName,
      @PathVariable Long orderItemUnitId,
      @RequestBody OrderItemUnitUpdateRequest request) {
    orderService.updateServedStatus(departmentName, orderItemUnitId, request);
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "주문 상세 개별 상태 변경에 성공했습니다.", null));
  }
}
