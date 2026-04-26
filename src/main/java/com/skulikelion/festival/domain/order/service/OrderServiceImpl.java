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
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.domain.booth.repository.BoothMenuRepository;
import com.skulikelion.festival.domain.booth.repository.BoothRepository;
import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.domain.manager.exception.ManagerErrorCode;
import com.skulikelion.festival.domain.manager.repository.ManagerRepository;
import com.skulikelion.festival.domain.order.dto.event.WaitingOrderPayload;
import com.skulikelion.festival.domain.order.dto.request.OrderCreateRequest;
import com.skulikelion.festival.domain.order.dto.request.OrderItemCreateRequest;
import com.skulikelion.festival.domain.order.dto.request.OrderItemUnitUpdateRequest;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderItemResponse;
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

  @Override
  @Transactional
  public OrderResponse createOrder(Long boothId, OrderCreateRequest request) {
    Booth booth = validateBoothExists(boothId);

    List<Long> boothMenuIds =
        request.getOrderItems().stream().map(OrderItemCreateRequest::getBoothMenuId).toList();
    List<BoothMenu> boothMenus = boothMenuRepository.findAllById(boothMenuIds);
    validateBoothMenusExistence(boothMenus.size(), boothMenuIds.size());

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
        orderMapper.toOrderItemResponseList(savedOrderItems);
    OrderResponse orderResponse =
        orderMapper.toOrderResponse(savedOrder, booth, orderItemResponses);

    WaitingOrderResponse waitingOrderResponse =
        orderMapper.toWaitingOrderResponse(savedOrder, savedOrderItems);

    WaitingOrderPayload waitingOrderPayload =
        orderEventMapper.toWaitingOrderPayload(booth, waitingOrderResponse);
    eventPublisher.publishEvent(waitingOrderPayload);

    log.info(
        "[OrderService] 주문 생성 성공 - 주문 식별자: {}, 주문자명: {}, 총 주문 금액: {}",
        order.getId(),
        order.getCustomerName(),
        order.getTotalOrderPrice());
    return orderResponse;
  }

  @Override
  @Transactional(readOnly = true)
  public List<WaitingOrderResponse> getWaitingOrders(String departmentName, Long boothId) {
    Booth booth = validateBoothExists(boothId);
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
  public List<CookingOrderResponse> getCookingOrders(String departmentName, Long boothId) {
    Booth booth = validateBoothExists(boothId);
    validateBoothManager(departmentName, booth);

    List<CookingOrderResponse> cookingOrders = orderRepository.findCookingOrdersByBoothId(boothId);
    List<Long> orderIds = cookingOrders.stream().map(CookingOrderResponse::getOrderId).toList();
    if (orderIds.isEmpty()) return cookingOrders;

    List<CookingOrderItemResponse> cookingOrderItems =
        orderItemRepository.findCookingOrderItemsByOrderIds(orderIds);
    List<Long> orderItemIds =
        cookingOrderItems.stream().map(CookingOrderItemResponse::getOrderItemId).toList();

    List<CookingOrderItemUnitResponse> cookingOrderItemUnits =
        orderItemUnitRepository.findCookingOrderItemUnitsByOrderItemIds(orderItemIds);

    Map<Long, List<CookingOrderItemResponse>> cookingItemMap =
        cookingOrderItems.stream()
            .collect(Collectors.groupingBy(CookingOrderItemResponse::getOrderId));
    Map<Long, List<CookingOrderItemUnitResponse>> cookingItemUnitMap =
        cookingOrderItemUnits.stream()
            .collect(Collectors.groupingBy(CookingOrderItemUnitResponse::getOrderItemId));

    cookingOrderItems.forEach(
        orderItem ->
            orderItem.addOrderItemUnits(
                cookingItemUnitMap.getOrDefault(orderItem.getOrderItemId(), List.of())));
    cookingOrders.forEach(
        order -> order.addOrderItems(cookingItemMap.getOrDefault(order.getOrderId(), List.of())));
    return cookingOrders;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CompletedOrderResponse> getCompletedOrders(
      String departmentName, Long boothId, LocalDate orderDate, String keyword) {
    Booth booth = validateBoothExists(boothId);
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
      String departmentName, Long boothId, LocalDate orderDate, String keyword) {
    Booth booth = validateBoothExists(boothId);
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
  public void updateOrderStatus(
      String departmentName, Long boothId, Long orderId, OrderStatus newOrderStatus) {
    Booth booth = validateBoothExists(boothId);
    validateBoothManager(departmentName, booth);
    Order order = validateOrderExists(orderId);

    OrderStatus previousStatus = order.getOrderStatus();
    order.changeOrderStatus(newOrderStatus);
    if (previousStatus != newOrderStatus) {
      switch (newOrderStatus) {
        case COOKING -> {
          List<CookingOrderItemResponse> cookingOrderItems =
              orderItemRepository.findCookingOrderItemsByOrderIds(List.of(order.getId()));

          if (cookingOrderItems.isEmpty()) {
            eventPublisher.publishEvent(
                orderEventMapper.toCookingOrderPayload(
                    booth, orderMapper.toCookingOrderResponse(order, List.of())));
            break;
          }
          List<Long> orderItemIds =
              cookingOrderItems.stream().map(CookingOrderItemResponse::getOrderItemId).toList();
          List<CookingOrderItemUnitResponse> cookingOrderItemUnits =
              orderItemUnitRepository.findCookingOrderItemUnitsByOrderItemIds(orderItemIds);

          Map<Long, List<CookingOrderItemUnitResponse>> cookingItemMap =
              cookingOrderItemUnits.stream()
                  .collect(Collectors.groupingBy(CookingOrderItemUnitResponse::getOrderItemId));

          cookingOrderItems.forEach(
              orderItem ->
                  orderItem.addOrderItemUnits(
                      cookingItemMap.getOrDefault(orderItem.getOrderItemId(), List.of())));
          CookingOrderResponse cookingOrderResponse =
              orderMapper.toCookingOrderResponse(order, cookingOrderItems);

          eventPublisher.publishEvent(
              orderEventMapper.toCookingOrderPayload(booth, cookingOrderResponse));
        }
        case COMPLETED -> {
          List<CompletedOrderItemResponse> completedOrderItems =
              orderItemRepository.findCompletedOrderItemsByOrderIds(List.of(order.getId()));

          if (completedOrderItems.isEmpty()) {
            eventPublisher.publishEvent(
                orderEventMapper.toCompletedOrderPayload(
                    booth, orderMapper.toCompletedOrderResponse(order, List.of())));
            break;
          }

          CompletedOrderResponse completedOrderResponse =
              orderMapper.toCompletedOrderResponse(order, completedOrderItems);

          eventPublisher.publishEvent(
              orderEventMapper.toCompletedOrderPayload(booth, completedOrderResponse));
        }
      }
    }
  }

  @Override
  @Transactional
  public void cancelOrder(
      String departmentName, Long boothId, Long orderId, OrderCancelReason orderCancelReason) {
    Booth booth = validateBoothExists(boothId);
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
      String departmentName,
      Long boothId,
      Long orderItemUnitId,
      OrderItemUnitUpdateRequest request) {
    Booth booth = validateBoothExists(boothId);
    validateBoothManager(departmentName, booth);
    validateOrderIsCooking(orderItemUnitId);
    OrderItemUnit orderItemUnit = validateOrderItemUnitExists(orderItemUnitId);

    boolean previousServed = orderItemUnit.isServed();
    orderItemUnit.updateServedStatus(request.isServed());

    if (previousServed != request.isServed()) {
      eventPublisher.publishEvent(
          orderEventMapper.toCookingOrderItemUnitPayload(
              booth, orderMapper.toCookingOrderItemUnitResponse(orderItemUnit)));
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
            throw new CustomException(OrderErrorCode.MENU_PRICE_MISMATCH);
          }
        });
  }

  private void validateBoothMenusExistence(int boothMenuSize, int boothMenuIdsSize) {
    if (boothMenuSize != boothMenuIdsSize) {
      throw new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  private Booth validateBoothExists(Long boothId) {
    return boothRepository
        .findById(boothId)
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
      throw new CustomException(OrderErrorCode.ORDER_STATUS_CHANGE_FAILED);
    }
  }

  private OrderItemUnit validateOrderItemUnitExists(Long orderItemUnitId) {
    return orderItemUnitRepository
        .findById(orderItemUnitId)
        .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_ITEM_UNIT_NOT_FOUND));
  }
}
