/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.entity.BoothMenu;
import com.skulikelion.festival.domain.booth.enums.TimeType;
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.domain.booth.repository.BoothMenuRepository;
import com.skulikelion.festival.domain.booth.repository.BoothRepository;
import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.domain.manager.exception.ManagerErrorCode;
import com.skulikelion.festival.domain.manager.repository.ManagerRepository;
import com.skulikelion.festival.domain.order.dto.event.OrderIdempotencyPayload;
import com.skulikelion.festival.domain.order.dto.event.WaitingOrderPayload;
import com.skulikelion.festival.domain.order.dto.request.OrderCreateRequest;
import com.skulikelion.festival.domain.order.dto.request.OrderItemCreateRequest;
import com.skulikelion.festival.domain.order.dto.request.OrderItemUnitUpdateRequest;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderItemUnitResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;
import com.skulikelion.festival.domain.order.entity.Order;
import com.skulikelion.festival.domain.order.entity.OrderItem;
import com.skulikelion.festival.domain.order.entity.OrderItemUnit;
import com.skulikelion.festival.domain.order.entity.enums.OrderCancelReason;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;
import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.domain.order.mapper.OrderEventMapper;
import com.skulikelion.festival.domain.order.mapper.OrderMapper;
import com.skulikelion.festival.domain.order.repository.OrderItemRepository;
import com.skulikelion.festival.domain.order.repository.OrderItemUnitRepository;
import com.skulikelion.festival.domain.order.repository.OrderRepository;
import com.skulikelion.festival.domain.order.service.idempotency.OrderIdempotencyService;
import com.skulikelion.festival.global.enums.Department;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.exception.GlobalErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;
  private final OrderMapper orderMapper;
  private final BoothMenuRepository boothMenuRepository;
  private final OrderItemRepository orderItemRepository;
  private final BoothRepository boothRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final OrderEventMapper orderEventMapper;
  private final ManagerRepository managerRepository;
  private final OrderItemUnitRepository orderItemUnitRepository;
  private final OrderIdempotencyService orderIdempotencyService;

  @Override
  @Transactional
  public OrderResponse createOrder(
      Long boothId, String idempotencyKey, OrderCreateRequest request) {
    if (!orderIdempotencyService.isNewRequest(idempotencyKey)) {
      return orderIdempotencyService.getCachedResponse(idempotencyKey, OrderResponse.class);
    }
    try {

      Booth booth = validateBoothExists(boothId);
      validateBoothUsesOrder(booth);

      List<Long> boothMenuIds =
          request.getOrderItems().stream().map(OrderItemCreateRequest::getBoothMenuId).toList();
      List<BoothMenu> boothMenus = boothMenuRepository.findAllById(boothMenuIds);
      validateBoothMenusExistence(boothMenus.size(), boothMenuIds.size());
      validateBoothMenusOrderable(booth, boothMenus);

      Map<Long, BoothMenu> boothMenuMap =
          boothMenus.stream().collect(Collectors.toMap(BoothMenu::getId, Function.identity()));
      validateMenuPrice(request.getOrderItems(), boothMenuMap);
      validateTotalPrice(request);

      Order order = orderMapper.createOrderFromOrderCreateRequest(request);
      Order savedOrder = orderRepository.save(order);

      List<OrderItem> orderItems =
          request.getOrderItems().stream()
              .map(
                  orderItemCreateRequest -> {
                    BoothMenu boothMenu = boothMenuMap.get(orderItemCreateRequest.getBoothMenuId());
                    return orderMapper.createOrderItemFromOrderItemCreateRequest(
                        orderItemCreateRequest, savedOrder, boothMenu);
                  })
              .toList();
      List<OrderItem> savedOrderItems = orderItemRepository.saveAll(orderItems);

      List<OrderItemUnit> orderItemUnits =
          savedOrderItems.stream()
              .flatMap(
                  orderItem ->
                      IntStream.range(0, orderItem.getQuantity())
                          .mapToObj(i -> orderMapper.createOrderItemUnitFromOrderItem(orderItem)))
              .toList();

      orderItemUnitRepository.saveAll(orderItemUnits);

      List<OrderItemResponse> orderItemResponses =
          orderMapper.toOrderItemResponseList(savedOrderItems, request.getLanguage());
      OrderResponse orderResponse =
          orderMapper.toOrderResponse(savedOrder, booth, orderItemResponses);

      WaitingOrderResponse waitingOrderResponse =
          orderMapper.toWaitingOrderResponse(savedOrder, savedOrderItems);

      WaitingOrderPayload waitingOrderPayload =
          orderEventMapper.toWaitingOrderPayload(booth, waitingOrderResponse);
      eventPublisher.publishEvent(waitingOrderPayload);

      OrderIdempotencyPayload orderIdempotencyPayload =
          orderEventMapper.toOrderIdempotencyPayload(idempotencyKey, orderResponse);
      eventPublisher.publishEvent(orderIdempotencyPayload);

      log.info(
          "[OrderService] 주문 생성 성공 - 주문 식별자: {}, 주문자명: {}, 총 주문 금액: {}",
          order.getId(),
          order.getCustomerName(),
          order.getTotalOrderPrice());
      return orderResponse;
    } catch (Exception e) {
      orderIdempotencyService.deleteKey(idempotencyKey);
      throw e;
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<WaitingOrderResponse> getWaitingOrders(String departmentName) {
    Booth booth = validateBoothExists(departmentName);
    Long boothId = booth.getId();
    validateBoothUsesOrder(booth);
    validateBoothManager(departmentName, booth);

    List<WaitingOrderResponse> waitingOrders = orderRepository.findWaitingOrdersByBoothId(boothId);
    List<Long> orderIds = waitingOrders.stream().map(WaitingOrderResponse::getOrderId).toList();
    if (orderIds.isEmpty()) return waitingOrders;

    List<WaitingOrderItemResponse> waitingOrderItems =
        orderItemRepository.findWaitingOrderItemsByOrderIds(orderIds);
    Map<Long, List<WaitingOrderItemResponse>> waitingItemMap =
        waitingOrderItems.stream()
            .collect(Collectors.groupingBy(WaitingOrderItemResponse::getOrderId));

    waitingOrders.forEach(
        order -> order.addOrderItems(waitingItemMap.getOrDefault(order.getOrderId(), List.of())));

    return waitingOrders;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CookingOrderResponse> getCookingOrders(String departmentName) {
    Booth booth = validateBoothExists(departmentName);
    Long boothId = booth.getId();
    validateBoothUsesOrder(booth);
    validateBoothManager(departmentName, booth);

    List<CookingOrderResponse> cookingOrders = orderRepository.findCookingOrdersByBoothId(boothId);
    List<Long> orderIds = cookingOrders.stream().map(CookingOrderResponse::getOrderId).toList();
    if (orderIds.isEmpty()) return cookingOrders;

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
    return cookingOrders;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CompletedOrderResponse> getCompletedOrders(
      String departmentName, LocalDate orderDate, String keyword) {
    Booth booth = validateBoothExists(departmentName);
    Long boothId = booth.getId();
    validateBoothUsesOrder(booth);
    validateBoothManager(departmentName, booth);

    List<CompletedOrderResponse> completedOrders =
        orderRepository.findCompletedOrdersByBoothIdAndDateAndKeyword(boothId, orderDate, keyword);
    List<Long> orderIds = completedOrders.stream().map(CompletedOrderResponse::getOrderId).toList();
    if (orderIds.isEmpty()) return completedOrders;

    List<CompletedOrderItemResponse> completedOrderItems =
        orderItemRepository.findCompletedOrderItemsByOrderIds(orderIds);
    Map<Long, List<CompletedOrderItemResponse>> completedItemMap =
        completedOrderItems.stream()
            .collect(Collectors.groupingBy(CompletedOrderItemResponse::getOrderId));

    completedOrders.forEach(
        order -> order.addOrderItems(completedItemMap.getOrDefault(order.getOrderId(), List.of())));

    return completedOrders;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CanceledOrderResponse> getCanceledOrders(
      String departmentName, LocalDate orderDate, String keyword) {
    Booth booth = validateBoothExists(departmentName);
    Long boothId = booth.getId();
    validateBoothUsesOrder(booth);
    validateBoothManager(departmentName, booth);

    List<CanceledOrderResponse> canceledOrders =
        orderRepository.findCanceledOrdersByBoothIdAndDateAndKeyword(boothId, orderDate, keyword);
    List<Long> orderIds = canceledOrders.stream().map(CanceledOrderResponse::getOrderId).toList();
    if (orderIds.isEmpty()) return canceledOrders;

    List<CanceledOrderItemResponse> canceledOrderItems =
        orderItemRepository.findCanceledOrderItemsByOrderIds(orderIds);
    Map<Long, List<CanceledOrderItemResponse>> canceledItemMap =
        canceledOrderItems.stream()
            .collect(Collectors.groupingBy(CanceledOrderItemResponse::getOrderId));

    canceledOrders.forEach(
        order -> order.addOrderItems(canceledItemMap.getOrDefault(order.getOrderId(), List.of())));

    return canceledOrders;
  }

  @Override
  @Transactional
  public void updateOrderStatus(String departmentName, Long orderId, OrderStatus newOrderStatus) {
    Booth booth = validateBoothExists(departmentName);
    validateBoothUsesOrder(booth);
    validateBoothManager(departmentName, booth);
    Order order = validateOrderExists(orderId);

    OrderStatus previousStatus = order.getOrderStatus();
    order.changeOrderStatus(newOrderStatus);
    if (previousStatus != newOrderStatus) {
      switch (newOrderStatus) {
        case COOKING -> {
          List<Long> orderItemIds =
              orderItemRepository.findOrderItemIdsByOrderIds(List.of(orderId));

          List<CookingOrderItemUnitResponse> cookingOrderItemUnits =
              orderItemIds.isEmpty()
                  ? List.of()
                  : orderItemUnitRepository.findCookingOrderItemUnitsByOrderItemIds(orderItemIds);

          CookingOrderResponse cookingOrderResponse =
              orderMapper.toCookingOrderResponse(order, cookingOrderItemUnits);

          eventPublisher.publishEvent(
              orderEventMapper.toCookingOrderPayload(booth, cookingOrderResponse));
        }
        case COMPLETED -> {
          List<CompletedOrderItemResponse> completedOrderItems =
              orderItemRepository.findCompletedOrderItemsByOrderIds(List.of(order.getId()));

          eventPublisher.publishEvent(
              orderEventMapper.toCompletedOrderPayload(
                  booth, orderMapper.toCompletedOrderResponse(order, completedOrderItems)));
        }
      }
    }
  }

  @Override
  @Transactional
  public void cancelOrder(
      String departmentName, Long orderId, OrderCancelReason orderCancelReason) {
    Booth booth = validateBoothExists(departmentName);
    validateBoothUsesOrder(booth);
    validateBoothManager(departmentName, booth);
    Order order = validateOrderExists(orderId);

    order.cancelOrder(orderCancelReason);

    List<CanceledOrderItemResponse> canceledOrderItems =
        orderItemRepository.findCanceledOrderItemsByOrderIds(List.of(order.getId()));

    if (canceledOrderItems.isEmpty()) {
      eventPublisher.publishEvent(
          orderEventMapper.toCanceledOrderPayload(
              booth, orderMapper.toCanceledOrderResponse(order, List.of(), orderCancelReason)));
      return;
    }

    CanceledOrderResponse canceledOrderResponse =
        orderMapper.toCanceledOrderResponse(order, canceledOrderItems, orderCancelReason);

    eventPublisher.publishEvent(
        orderEventMapper.toCanceledOrderPayload(booth, canceledOrderResponse));
  }

  @Override
  @Transactional
  public void updateServedStatus(
      String departmentName, Long orderItemUnitId, OrderItemUnitUpdateRequest request) {
    Booth booth = validateBoothExists(departmentName);
    validateBoothUsesOrder(booth);
    validateBoothManager(departmentName, booth);
    validateOrderIsCooking(orderItemUnitId);
    OrderItemUnit orderItemUnit = validateOrderItemUnitExists(orderItemUnitId);

    boolean previousServed = orderItemUnit.isServed();
    orderItemUnit.updateServedStatus(request.isServed());

    if (previousServed != request.isServed()) {
      eventPublisher.publishEvent(
          orderEventMapper.toCookingOrderItemUnitPayload(
              booth, orderMapper.toOrderItemUnitStatusResponse(orderItemUnit)));
    }
  }

  private void validateTotalPrice(OrderCreateRequest request) {
    int sum =
        request.getOrderItems().stream()
            .mapToInt(
                item -> {
                  if (item.getMenuPrice() * item.getQuantity() != item.getTotalOrderItemPrice()) {
                    throw new CustomException(OrderErrorCode.ORDER_ITEM_TOTAL_PRICE_MISMATCH);
                  }
                  return item.getTotalOrderItemPrice();
                })
            .sum();
    if (request.getTotalOrderPrice() != sum) {
      throw new CustomException(OrderErrorCode.ORDER_TOTAL_PRICE_MISMATCH);
    }
  }

  private void validateMenuPrice(
      List<OrderItemCreateRequest> orderItems, Map<Long, BoothMenu> boothMenuMap) {
    orderItems.forEach(
        item -> {
          BoothMenu boothMenu = boothMenuMap.get(item.getBoothMenuId());
          if (!boothMenu.getPrice().equals(item.getMenuPrice())) {
            log.warn(
                "[OrderService] 주문한 메뉴 가격이 실제 가격과 일치하지 않습니다 - 주문 메뉴 가격: {}, 실제 메뉴 가격: {}",
                item.getMenuPrice(),
                boothMenu.getPrice());
            throw new CustomException(OrderErrorCode.ORDER_MENU_PRICE_MISMATCH);
          }
        });
  }

  private void validateBoothMenusExistence(int boothMenuSize, int boothMenuIdsSize) {
    if (boothMenuSize != boothMenuIdsSize) {
      log.info("boothMenuSize: {}, boothMenuIdsSize: {}", boothMenuSize, boothMenuIdsSize);
      throw new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  private Booth validateBoothExists(Long boothId) {
    return boothRepository
        .findById(boothId)
        .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));
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

  private Order validateOrderExists(Long orderId) {
    return orderRepository
        .findById(orderId)
        .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));
  }

  private void validateOrderIsCooking(Long orderItemUnitId) {
    OrderStatus status =
        orderItemUnitRepository
            .findOrderStatusByOrderItemUnitId(orderItemUnitId)
            .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_ITEM_UNIT_NOT_FOUND));
    if (status != OrderStatus.COOKING) {
      throw new CustomException(OrderErrorCode.ORDER_ITEM_UNIT_STATUS_CHANGE_FAILED);
    }
  }

  private OrderItemUnit validateOrderItemUnitExists(Long orderItemUnitId) {
    return orderItemUnitRepository
        .findById(orderItemUnitId)
        .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_ITEM_UNIT_NOT_FOUND));
  }

  private void validateBoothUsesOrder(Booth booth) {
    if (!booth.isOrderEnabled()) {
      throw new CustomException(OrderErrorCode.ORDER_NOT_USED_BOOTH);
    }
  }

  private void validateBoothMenusOrderable(Booth booth, List<BoothMenu> boothMenus) {
    // 해당 부스가 주문 받는 시간대인지 검증 필요
    validateBoothMenusTimeType(boothMenus);
    validateBoothMenusSoldOut(boothMenus);
  }

  private void validateBoothMenusSoldOut(List<BoothMenu> boothMenus) {
    boolean hasSoldOut = boothMenus.stream().anyMatch(BoothMenu::getIsSoldOut);
    if (hasSoldOut) {
      throw new CustomException(OrderErrorCode.ORDER_MENU_SOLD_OUT);
    }
  }

  private void validateBoothMenusTimeType(List<BoothMenu> boothMenus) {
    boolean hasDayMenu = boothMenus.stream().anyMatch(menu -> menu.getTimeType() == TimeType.DAY);
    if (hasDayMenu) {
      throw new CustomException(OrderErrorCode.ORDER_DAY_TYPE_MENU);
    }
  }
}
