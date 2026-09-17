/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.domain.booth.repository.BoothRepository;
import com.skulikelion.festival.domain.order.dto.request.CanceledOrderCursor;
import com.skulikelion.festival.domain.order.dto.request.CompletedOrderCursor;
import com.skulikelion.festival.domain.order.dto.request.CookingOrderCursor;
import com.skulikelion.festival.domain.order.dto.request.OrderCreateRequest;
import com.skulikelion.festival.domain.order.dto.request.OrderItemUnitUpdateRequest;
import com.skulikelion.festival.domain.order.dto.request.WaitingOrderCursor;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderItemUnitResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderResponse;
import com.skulikelion.festival.domain.order.dto.response.SalesResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;
import com.skulikelion.festival.domain.order.entity.Order;
import com.skulikelion.festival.domain.order.entity.OrderItemUnit;
import com.skulikelion.festival.domain.order.entity.enums.OrderCancelReason;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;
import com.skulikelion.festival.domain.order.event.payload.OrderStatusChangedEvent;
import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.domain.order.mapper.OrderEventMapper;
import com.skulikelion.festival.domain.order.mapper.OrderMapper;
import com.skulikelion.festival.domain.order.repository.OrderItemRepository;
import com.skulikelion.festival.domain.order.repository.OrderItemUnitRepository;
import com.skulikelion.festival.domain.order.repository.OrderRepository;
import com.skulikelion.festival.domain.order.service.processor.OrderProcessor;
import com.skulikelion.festival.global.common.pagenation.CursorPage;
import com.skulikelion.festival.global.common.pagenation.CursorPageResponse;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.security.AuthPrincipal;
import com.skulikelion.festival.global.security.BoothOwnershipValidator;
import com.skulikelion.festival.global.util.idempotency.annotation.Idempotent;
import com.skulikelion.festival.global.util.idempotency.strategy.IdempotencyType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;
  private final OrderMapper orderMapper;
  private final OrderItemRepository orderItemRepository;
  private final BoothRepository boothRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final OrderEventMapper orderEventMapper;
  private final OrderItemUnitRepository orderItemUnitRepository;
  private final OrderProcessor orderProcessor;
  private final BoothOwnershipValidator boothOwnershipValidator;

  @Override
  @Idempotent(idempotencyKey = "#idempotencyKey", strategy = IdempotencyType.FALLBACK)
  public OrderResponse createOrder(
      Long boothId, String idempotencyKey, OrderCreateRequest request) {
    return orderProcessor.processOrder(boothId, request);
  }

  @Override
  @Transactional(readOnly = true)
  public CursorPageResponse<WaitingOrderResponse> getWaitingOrders(
      AuthPrincipal principal, WaitingOrderCursor cursor, Integer size) {
    Booth booth = getBoothOrThrow(principal.departmentId());
    Long boothId = booth.getId();
    validateBoothManagerBelongsToBooth(principal, booth);

    List<WaitingOrderResponse> waitingOrders =
        orderRepository.findWaitingOrdersByBoothId(
            boothId,
            cursor != null ? cursor.lastCreatedAt() : null,
            cursor != null ? cursor.lastOrderId() : null,
            size + 1);
    CursorPage<WaitingOrderResponse> page = CursorPage.of(waitingOrders, size);

    // early return
    if (waitingOrders.isEmpty()) return orderMapper.toWaitingOrderResponseCursorPage(page);

    List<Long> orderIds = waitingOrders.stream().map(WaitingOrderResponse::getOrderId).toList();
    List<WaitingOrderItemResponse> waitingOrderItems =
        orderItemRepository.findWaitingOrderItemsByOrderIds(orderIds);
    Map<Long, List<WaitingOrderItemResponse>> waitingItemMap =
        waitingOrderItems.stream()
            .collect(Collectors.groupingBy(WaitingOrderItemResponse::getOrderId));

    waitingOrders.forEach(
        order -> order.addOrderItems(waitingItemMap.getOrDefault(order.getOrderId(), List.of())));

    log.debug(
        "[OrderService] 대기 주문 조회 완료 - 학과 식별자: {}, 주문 수: {}",
        principal.departmentId(),
        waitingOrders.size());
    return orderMapper.toWaitingOrderResponseCursorPage(page);
  }

  @Override
  @Transactional(readOnly = true)
  public CursorPageResponse<CookingOrderResponse> getCookingOrders(
      AuthPrincipal principal, CookingOrderCursor cursor, Integer size) {
    Booth booth = getBoothOrThrow(principal.departmentId());
    Long boothId = booth.getId();
    validateBoothManagerBelongsToBooth(principal, booth);

    List<CookingOrderResponse> cookingOrders =
        orderRepository.findCookingOrdersByBoothId(
            boothId,
            cursor != null ? cursor.lastModifiedAt() : null,
            cursor != null ? cursor.lastOrderId() : null,
            size + 1);
    CursorPage<CookingOrderResponse> page = CursorPage.of(cookingOrders, size);

    // early return
    if (cookingOrders.isEmpty()) return orderMapper.toCookingOrderResponseCursorPage(page);

    List<Long> orderIds = cookingOrders.stream().map(CookingOrderResponse::getOrderId).toList();
    List<Long> orderItemIds = orderItemRepository.findOrderItemIdsByOrderIds(orderIds);
    List<CookingOrderItemUnitResponse> cookingOrderItemUnits =
        orderItemUnitRepository.findCookingOrderItemUnitsByOrderItemIds(orderItemIds);

    Map<Long, List<CookingOrderItemUnitResponse>> cookingItemUnitMap =
        cookingOrderItemUnits.stream()
            .collect(Collectors.groupingBy(CookingOrderItemUnitResponse::getOrderId));

    cookingOrders.forEach(
        order ->
            order.addOrderItemUnits(
                cookingItemUnitMap.getOrDefault(order.getOrderId(), List.of())));

    log.debug(
        "[OrderService] 조리 주문 조회 완료 - 학과 식별자: {}, 주문 수: {}",
        principal.departmentId(),
        cookingOrders.size());
    return orderMapper.toCookingOrderResponseCursorPage(page);
  }

  @Override
  @Transactional(readOnly = true)
  public CursorPageResponse<CompletedOrderResponse> getCompletedOrders(
      AuthPrincipal principal,
      LocalDate orderDate,
      String keyword,
      CompletedOrderCursor cursor,
      Integer size) {
    Booth booth = getBoothOrThrow(principal.departmentId());
    Long boothId = booth.getId();
    validateBoothManagerBelongsToBooth(principal, booth);

    List<CompletedOrderResponse> completedOrders =
        orderRepository.findCompletedOrdersByBoothIdAndDateAndKeyword(
            boothId,
            orderDate,
            keyword,
            cursor != null ? cursor.lastCompletedAt() : null,
            cursor != null ? cursor.lastOrderId() : null,
            size + 1);
    CursorPage<CompletedOrderResponse> page = CursorPage.of(completedOrders, size);

    // early return
    if (completedOrders.isEmpty()) return orderMapper.toCompletedOrderResponseCursorPage(page);

    List<Long> orderIds = completedOrders.stream().map(CompletedOrderResponse::getOrderId).toList();
    List<CompletedOrderItemResponse> completedOrderItems =
        orderItemRepository.findCompletedOrderItemsByOrderIds(orderIds);
    Map<Long, List<CompletedOrderItemResponse>> completedItemMap =
        completedOrderItems.stream()
            .collect(Collectors.groupingBy(CompletedOrderItemResponse::getOrderId));

    completedOrders.forEach(
        order -> order.addOrderItems(completedItemMap.getOrDefault(order.getOrderId(), List.of())));

    log.debug(
        "[OrderService] 완료 주문 조회 완료 - 학과 식별자: {}, 주문 수: {}",
        principal.departmentId(),
        completedOrders.size());
    return orderMapper.toCompletedOrderResponseCursorPage(page);
  }

  @Override
  @Transactional(readOnly = true)
  public CursorPageResponse<CanceledOrderResponse> getCanceledOrders(
      AuthPrincipal principal,
      LocalDate orderDate,
      String keyword,
      CanceledOrderCursor cursor,
      Integer size) {
    Booth booth = getBoothOrThrow(principal.departmentId());
    Long boothId = booth.getId();
    validateBoothManagerBelongsToBooth(principal, booth);

    List<CanceledOrderResponse> canceledOrders =
        orderRepository.findCanceledOrdersByBoothIdAndDateAndKeyword(
            boothId,
            orderDate,
            keyword,
            cursor != null ? cursor.lastCanceledAt() : null,
            cursor != null ? cursor.lastOrderId() : null,
            size + 1);
    CursorPage<CanceledOrderResponse> page = CursorPage.of(canceledOrders, size);

    // early return
    if (canceledOrders.isEmpty()) return orderMapper.toCanceledOrderResponseCursorPage(page);

    List<Long> orderIds = canceledOrders.stream().map(CanceledOrderResponse::getOrderId).toList();
    List<CanceledOrderItemResponse> canceledOrderItems =
        orderItemRepository.findCanceledOrderItemsByOrderIds(orderIds);
    Map<Long, List<CanceledOrderItemResponse>> canceledItemMap =
        canceledOrderItems.stream()
            .collect(Collectors.groupingBy(CanceledOrderItemResponse::getOrderId));

    canceledOrders.forEach(
        order -> order.addOrderItems(canceledItemMap.getOrDefault(order.getOrderId(), List.of())));

    log.debug(
        "[OrderService] 취소 주문 조회 완료 - 학과 식별자: {}, 주문 수: {}",
        principal.departmentId(),
        canceledOrders.size());
    return orderMapper.toCanceledOrderResponseCursorPage(page);
  }

  @Override
  @Transactional(readOnly = true)
  public SalesResponse getSales(AuthPrincipal principal, LocalDate date) {
    Booth booth = getBoothOrThrow(principal.departmentId());
    Long boothId = booth.getId();
    validateBoothManagerBelongsToBooth(principal, booth);

    if (date == null) {
      return orderRepository.findCompletedOrderTotalAmountByBoothId(boothId);
    }

    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.plusDays(1).atStartOfDay();

    return orderRepository.findCompletedOrderTotalAmountByBoothIdAndDate(boothId, start, end);
  }

  @Override
  @Transactional
  public void updateOrderStatus(AuthPrincipal principal, Long orderId, OrderStatus newOrderStatus) {
    Booth booth = getBoothOrThrow(principal.departmentId());
    validateBoothManagerBelongsToBooth(principal, booth);
    Order order = getOrderOrThrow(orderId);

    OrderStatus previousStatus = order.getOrderStatus();
    order.changeOrderStatus(newOrderStatus);
    if (previousStatus == newOrderStatus) {
      log.debug(
          "[OrderService] 주문 상태 변경 요청이 현재 상태와 동일 - 주문 식별자: {}, 상태: {}", orderId, newOrderStatus);
      return;
    }

    log.info(
        "[OrderService] 주문 상태 변경 - 학과 식별자: {}, 주문 식별자: {}, 변경 전: {}, 변경 후: {}",
        principal.departmentId(),
        orderId,
        previousStatus,
        newOrderStatus);
    eventPublisher.publishEvent(
        new OrderStatusChangedEvent(booth.getId(), orderId, previousStatus, newOrderStatus, order));
  }

  @Override
  @Transactional
  public void cancelOrder(
      AuthPrincipal principal, Long orderId, OrderCancelReason orderCancelReason) {
    Booth booth = getBoothOrThrow(principal.departmentId());
    validateBoothManagerBelongsToBooth(principal, booth);
    Order order = getOrderOrThrow(orderId);

    OrderStatus previousStatus = order.getOrderStatus();
    if (previousStatus == OrderStatus.CANCELED) {
      log.debug("[OrderService] 주문 취소 요청이 현재 상태와 동일 - 주문 식별자: {}", orderId);
      return;
    }
    order.cancelOrder(orderCancelReason);

    log.info(
        "[OrderService] 주문 취소 - 학과 식별자: {}, 주문 식별자: {}, 취소 사유: {}",
        principal.departmentId(),
        orderId,
        orderCancelReason);

    List<CanceledOrderItemResponse> canceledOrderItems =
        orderItemRepository.findCanceledOrderItemsByOrderIds(List.of(order.getId()));

    if (canceledOrderItems.isEmpty()) {
      eventPublisher.publishEvent(
          orderEventMapper.toCanceledOrderPayload(
              booth.getId(),
              orderMapper.toCanceledOrderResponseFromDto(order, List.of(), orderCancelReason)));
      eventPublisher.publishEvent(
          orderEventMapper.toDismissOrderPayload(booth.getId(), previousStatus, orderId));
      log.debug("[OrderService] 취소 이벤트 발행 (주문 항목 없음) - 주문 식별자: {}", orderId);
      return;
    }

    CanceledOrderResponse canceledOrderResponse =
        orderMapper.toCanceledOrderResponseFromDto(order, canceledOrderItems, orderCancelReason);

    eventPublisher.publishEvent(
        orderEventMapper.toCanceledOrderPayload(booth.getId(), canceledOrderResponse));
    eventPublisher.publishEvent(
        orderEventMapper.toDismissOrderPayload(booth.getId(), previousStatus, orderId));
    log.debug(
        "[OrderService] 취소 이벤트 발행 - 주문 식별자: {}, 취소 항목 수: {}", orderId, canceledOrderItems.size());
  }

  @Override
  @Transactional
  public void updateServedStatus(
      AuthPrincipal principal, Long orderItemUnitId, OrderItemUnitUpdateRequest request) {
    Booth booth = getBoothOrThrow(principal.departmentId());
    validateBoothManagerBelongsToBooth(principal, booth);
    validateOrderIsCooking(orderItemUnitId);
    OrderItemUnit orderItemUnit = getOrderItemUnitOrThrow(orderItemUnitId);

    boolean previousServed = orderItemUnit.isServed();
    orderItemUnit.updateServedStatus(request.isServed());

    if (previousServed == request.isServed()) {
      log.debug(
          "[OrderService] 서빙 상태 변경 요청이 현재 상태와 동일 - 주문 항목 단위 식별자: {}, 상태: {}",
          orderItemUnitId,
          request.isServed());
      return;
    }

    log.info(
        "[OrderService] 서빙 상태 변경 - 학과 식별자: {}, 주문 항목 단위 식별자: {}, 변경 후: {}",
        principal.departmentId(),
        orderItemUnitId,
        request.isServed());
    eventPublisher.publishEvent(
        orderEventMapper.toCookingOrderItemUnitPayload(
            booth.getId(), orderMapper.toOrderItemUnitStatusResponse(orderItemUnit)));
    log.debug("[OrderService] 서빙 상태 변경 이벤트 발행 - 주문 항목 단위 식별자: {}", orderItemUnitId);
  }

  private Booth getBoothOrThrow(Long departmentId) {
    return boothRepository
        .findByDepartmentId(departmentId)
        .orElseThrow(
            () -> {
              log.warn("[OrderService] 부스를 찾을 수 없습니다 - 학과 식별자: {}", departmentId);
              return new CustomException(BoothErrorCode.BOOTH_NOT_FOUND);
            });
  }

  /**
   * [ 주문 자원의 부스 소유권 검증 메서드 ]
   *
   * <p>ADMIN 우회가 없다. 이는 이 경로의 기존 동작을 그대로 보존한 것이다 (LLD-0001 2.3 R6, 14장 O1).
   */
  private void validateBoothManagerBelongsToBooth(AuthPrincipal principal, Booth booth) {
    boothOwnershipValidator.validateOwner(principal, booth, OrderErrorCode.BOOTH_ACCESS_DENIED);
  }

  private Order getOrderOrThrow(Long orderId) {
    return orderRepository
        .findById(orderId)
        .orElseThrow(
            () -> {
              log.warn("[OrderService] 주문을 찾을 수 없습니다 - 주문 식별자: {}", orderId);
              return new CustomException(OrderErrorCode.ORDER_NOT_FOUND);
            });
  }

  private void validateOrderIsCooking(Long orderItemUnitId) {
    OrderStatus status =
        orderItemUnitRepository
            .findOrderStatusByOrderItemUnitId(orderItemUnitId)
            .orElseThrow(
                () -> {
                  log.warn(
                      "[OrderService] 주문 항목 단위를 찾을 수 없습니다 - 주문 항목 단위 식별자: {}", orderItemUnitId);
                  return new CustomException(OrderErrorCode.ORDER_ITEM_UNIT_NOT_FOUND);
                });
    if (status != OrderStatus.COOKING) {
      log.warn(
          "[OrderService] 조리 중이 아닌 주문 항목 단위 상태 변경 시도 - 주문 항목 단위 식별자: {}, 현재 상태: {}",
          orderItemUnitId,
          status);
      throw new CustomException(OrderErrorCode.ORDER_ITEM_UNIT_STATUS_CHANGE_FAILED);
    }
  }

  public OrderItemUnit getOrderItemUnitOrThrow(Long orderItemUnitId) {
    return orderItemUnitRepository
        .findById(orderItemUnitId)
        .orElseThrow(
            () -> {
              log.warn("[OrderService] 주문 항목 단위를 찾을 수 없습니다 - 주문 항목 단위 식별자: {}", orderItemUnitId);
              return new CustomException(OrderErrorCode.ORDER_ITEM_UNIT_NOT_FOUND);
            });
  }
}
