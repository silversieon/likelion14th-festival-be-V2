/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import com.skulikelion.festival.domain.booth.entity.BoothOperation;
import com.skulikelion.festival.domain.booth.enums.TimeType;
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.domain.booth.repository.BoothMenuRepository;
import com.skulikelion.festival.domain.booth.repository.BoothOperationRepository;
import com.skulikelion.festival.domain.booth.repository.BoothRepository;
import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.exception.ManagerErrorCode;
import com.skulikelion.festival.domain.manager.repository.ManagerRepository;
import com.skulikelion.festival.domain.order.dto.payload.OrderIdempotencyPayload;
import com.skulikelion.festival.domain.order.dto.payload.WaitingOrderPayload;
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
import com.skulikelion.festival.domain.order.dto.response.SalesResponse;
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
  private final BoothOperationRepository boothOperationRepository;

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
      BoothOperation boothOperation = validateBoothOrderTime(booth);

      List<Long> boothMenuIds =
          request.getOrderItems().stream().map(OrderItemCreateRequest::getBoothMenuId).toList();
      List<BoothMenu> boothMenus = boothMenuRepository.findAllById(boothMenuIds);
      validateBoothMenusExistence(boothMenus.size(), boothMenuIds.size());
      validateBoothMenusOrderable(boothMenus, boothOperation);

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
          "[OrderService] 주문 생성 성공 - 주문 식별자: {}, 학과명: {}, 주문자명: {}, 총 주문 금액: {}",
          order.getId(),
          booth.getDepartment().getDescription(),
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

    log.debug(
        "[OrderService] 대기 주문 조회 완료 - 학과명: {}, 주문 수: {}", departmentName, waitingOrders.size());
    return waitingOrders;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CookingOrderResponse> getCookingOrders(String departmentName) {
    Booth booth = validateBoothExists(departmentName);
    Long boothId = booth.getId();
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

    log.debug(
        "[OrderService] 조리 주문 조회 완료 - 학과명: {}, 주문 수: {}", departmentName, cookingOrders.size());
    return cookingOrders;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CompletedOrderResponse> getCompletedOrders(
      String departmentName, LocalDate orderDate, String keyword) {
    Booth booth = validateBoothExists(departmentName);
    Long boothId = booth.getId();
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

    log.debug(
        "[OrderService] 완료 주문 조회 완료 - 학과명: {}, 주문 수: {}", departmentName, completedOrders.size());
    return completedOrders;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CanceledOrderResponse> getCanceledOrders(
      String departmentName, LocalDate orderDate, String keyword) {
    Booth booth = validateBoothExists(departmentName);
    Long boothId = booth.getId();
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

    log.debug(
        "[OrderService] 취소 주문 조회 완료 - 학과명: {}, 주문 수: {}", departmentName, canceledOrders.size());
    return canceledOrders;
  }

  @Override
  @Transactional(readOnly = true)
  public SalesResponse getSales(String departmentName, LocalDate date) {
    Booth booth = validateBoothExists(departmentName);
    Long boothId = booth.getId();
    validateBoothManager(departmentName, booth);

    if (date == null) {
      return orderRepository.findCompletedOrderTotalAmountByBoothId(boothId);
    }

    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.plusDays(1).atStartOfDay();

    return orderRepository.findCompletedOrderTotalAmountByBoothIdAndDate(boothId, start, end);
  }

  @Override
  @Transactional
  public void updateOrderStatus(String departmentName, Long orderId, OrderStatus newOrderStatus) {
    Booth booth = validateBoothExists(departmentName);
    validateBoothManager(departmentName, booth);
    Order order = validateOrderExists(orderId);

    OrderStatus previousStatus = order.getOrderStatus();
    order.changeOrderStatus(newOrderStatus);
    if (previousStatus == newOrderStatus) {
      log.debug(
          "[OrderService] 주문 상태 변경 요청이 현재 상태와 동일 - 주문 식별자: {}, 상태: {}", orderId, newOrderStatus);
      return;
    }

    log.info(
        "[OrderService] 주문 상태 변경 - 학과명: {}, 주문 식별자: {}, 변경 전: {}, 변경 후: {}",
        departmentName,
        orderId,
        previousStatus,
        newOrderStatus);
    switch (newOrderStatus) {
      case WAITING -> {
        List<WaitingOrderItemResponse> waitingOrderItems =
            orderItemRepository.findWaitingOrderItemsByOrderIds(List.of(orderId));

        eventPublisher.publishEvent(
            orderEventMapper.toWaitingOrderPayload(
                booth, orderMapper.toWaitingOrderResponseFromDto(order, waitingOrderItems)));
        eventPublisher.publishEvent(
            orderEventMapper.toDismissOrderPayload(booth, previousStatus, orderId));
        log.debug("[OrderService] 취소 > 대기 이벤트 발행 - 주문 식별자: {}", orderId);
      }
      case COOKING -> {
        List<Long> orderItemIds = orderItemRepository.findOrderItemIdsByOrderIds(List.of(orderId));

        List<CookingOrderItemUnitResponse> cookingOrderItemUnits =
            orderItemIds.isEmpty()
                ? List.of()
                : orderItemUnitRepository.findCookingOrderItemUnitsByOrderItemIds(orderItemIds);

        CookingOrderResponse cookingOrderResponse =
            orderMapper.toCookingOrderResponseFromDto(order, cookingOrderItemUnits);

        eventPublisher.publishEvent(
            orderEventMapper.toCookingOrderPayload(
                booth, cookingOrderResponse, previousStatus, newOrderStatus));
        eventPublisher.publishEvent(
            orderEventMapper.toDismissOrderPayload(booth, previousStatus, orderId));
        // 만약 대기 중에서 보낸 거라면, 대기중 탭을 제외한 사람들에게 알림 숫자 2 라면 1로 바뀌게 필요
        log.debug("[OrderService] 조리 중 이벤트 발행 - 주문 식별자: {}", orderId);
      }
      case COMPLETED -> {
        List<CompletedOrderItemResponse> completedOrderItems =
            orderItemRepository.findCompletedOrderItemsByOrderIds(List.of(order.getId()));

        eventPublisher.publishEvent(
            orderEventMapper.toCompletedOrderPayload(
                booth,
                orderMapper.toCompletedOrderResponseFromDto(order, completedOrderItems),
                previousStatus,
                newOrderStatus));
        eventPublisher.publishEvent(
            orderEventMapper.toDismissOrderPayload(booth, previousStatus, orderId));
        // 만약 조리 중에서 보낸 거라면, 조리중 탭을 제외한 사람들에게 조리중 탭의 알림 숫자를 2 라면 1로 바뀌게 필요
        log.debug("[OrderService] 조리 > 완료 이벤트 발행 - 주문 식별자: {}", orderId);
      }
    }
  }

  @Override
  @Transactional
  public void cancelOrder(
      String departmentName, Long orderId, OrderCancelReason orderCancelReason) {
    Booth booth = validateBoothExists(departmentName);
    validateBoothManager(departmentName, booth);
    Order order = validateOrderExists(orderId);

    OrderStatus previousStatus = order.getOrderStatus();
    if (previousStatus == OrderStatus.CANCELED) {
      log.debug("[OrderService] 주문 취소 요청이 현재 상태와 동일 - 주문 식별자: {}", orderId);
      return;
    }
    order.cancelOrder(orderCancelReason);

    log.info(
        "[OrderService] 주문 취소 - 학과명: {}, 주문 식별자: {}, 취소 사유: {}",
        departmentName,
        orderId,
        orderCancelReason);

    List<CanceledOrderItemResponse> canceledOrderItems =
        orderItemRepository.findCanceledOrderItemsByOrderIds(List.of(order.getId()));

    if (canceledOrderItems.isEmpty()) {
      eventPublisher.publishEvent(
          orderEventMapper.toCanceledOrderPayload(
              booth,
              orderMapper.toCanceledOrderResponseFromDto(order, List.of(), orderCancelReason)));
      eventPublisher.publishEvent(
          orderEventMapper.toDismissOrderPayload(booth, previousStatus, orderId));
      log.debug("[OrderService] 취소 이벤트 발행 (주문 항목 없음) - 주문 식별자: {}", orderId);
      return;
    }

    CanceledOrderResponse canceledOrderResponse =
        orderMapper.toCanceledOrderResponseFromDto(order, canceledOrderItems, orderCancelReason);

    eventPublisher.publishEvent(
        orderEventMapper.toCanceledOrderPayload(booth, canceledOrderResponse));
    eventPublisher.publishEvent(
        orderEventMapper.toDismissOrderPayload(booth, previousStatus, orderId));
    log.debug(
        "[OrderService] 취소 이벤트 발행 - 주문 식별자: {}, 취소 항목 수: {}", orderId, canceledOrderItems.size());
  }

  @Override
  @Transactional
  public void updateServedStatus(
      String departmentName, Long orderItemUnitId, OrderItemUnitUpdateRequest request) {
    Booth booth = validateBoothExists(departmentName);
    validateBoothManager(departmentName, booth);
    validateOrderIsCooking(orderItemUnitId);
    OrderItemUnit orderItemUnit = validateOrderItemUnitExists(orderItemUnitId);

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
        "[OrderService] 서빙 상태 변경 - 학과명: {}, 주문 항목 단위 식별자: {}, 변경 후: {}",
        departmentName,
        orderItemUnitId,
        request.isServed());
    eventPublisher.publishEvent(
        orderEventMapper.toCookingOrderItemUnitPayload(
            booth, orderMapper.toOrderItemUnitStatusResponse(orderItemUnit)));
    log.debug("[OrderService] 서빙 상태 변경 이벤트 발행 - 주문 항목 단위 식별자: {}", orderItemUnitId);
  }

  private void validateTotalPrice(OrderCreateRequest request) {
    int sum =
        request.getOrderItems().stream()
            .mapToInt(
                item -> {
                  if (item.getMenuPrice() * item.getQuantity() != item.getTotalOrderItemPrice()) {
                    log.warn(
                        "[OrderService] 주문 항목 총 가격 불일치 - 메뉴 가격: {}, 수량: {}, 요청 총 가격: {}",
                        item.getMenuPrice(),
                        item.getQuantity(),
                        item.getTotalOrderItemPrice());
                    throw new CustomException(OrderErrorCode.ORDER_ITEM_TOTAL_PRICE_MISMATCH);
                  }
                  return item.getTotalOrderItemPrice();
                })
            .sum();
    if (request.getTotalOrderPrice() != sum) {
      log.warn(
          "[OrderService] 주문 총 가격 불일치 - 요청 총 가격: {}, 계산된 총 가격: {}",
          request.getTotalOrderPrice(),
          sum);
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
      log.warn(
          "[OrderService] 존재하지 않는 부스 메뉴 포함 - 요청 메뉴 수: {}, 실제 메뉴 수: {}",
          boothMenuIdsSize,
          boothMenuSize);
      throw new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  private Booth validateBoothExists(Long boothId) {
    return boothRepository
        .findById(boothId)
        .orElseThrow(
            () -> {
              log.warn("[OrderService] 부스를 찾을 수 없습니다 - 부스 식별자: {}", boothId);
              return new CustomException(BoothErrorCode.BOOTH_NOT_FOUND);
            });
  }

  private Booth validateBoothExists(String departmentName) {
    return boothRepository
        .findByDepartment(Department.valueOf(departmentName))
        .orElseThrow(
            () -> {
              log.warn("[OrderService] 부스를 찾을 수 없습니다 - 학과명: {}", departmentName);
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
                  log.warn("[OrderService] 매니저를 찾을 수 없습니다 - 학과명: {}", departmentName);
                  return new CustomException(ManagerErrorCode.MANAGER_NOT_FOUND);
                });
    if (!booth.getDepartment().equals(currentManager.getDepartment())) {
      log.warn(
          "[OrderService] 부스 접근 권한 없음 - 학과명: {}, 매니저 역할: {}",
          departmentName,
          currentManager.getRole());
      throw new CustomException(OrderErrorCode.BOOTH_ACCESS_DENIED);
    }
  }

  private Order validateOrderExists(Long orderId) {
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

  private OrderItemUnit validateOrderItemUnitExists(Long orderItemUnitId) {
    return orderItemUnitRepository
        .findById(orderItemUnitId)
        .orElseThrow(
            () -> {
              log.warn("[OrderService] 주문 항목 단위를 찾을 수 없습니다 - 주문 항목 단위 식별자: {}", orderItemUnitId);
              return new CustomException(OrderErrorCode.ORDER_ITEM_UNIT_NOT_FOUND);
            });
  }

  private void validateBoothUsesOrder(Booth booth) {
    if (!booth.isOrderEnabled()) {
      log.warn("[OrderService] 주문 미사용 부스에 주문 요청 - 학과명: {}", booth.getDepartment().getDescription());
      throw new CustomException(OrderErrorCode.ORDER_NOT_USED_BOOTH);
    }
  }

  private BoothOperation validateBoothOrderTime(Booth booth) {
    BoothOperation operation =
        boothOperationRepository
            .findByBoothIdAndOperationDate(booth.getId(), LocalDate.now())
            .orElseThrow(
                () -> {
                  log.warn(
                      "[OrderService] 오늘 부스 운영 정보를 찾을 수 없습니다 - 학과명: {}",
                      booth.getDepartment().getDescription());
                  return new CustomException(OrderErrorCode.ORDER_TIME_BOOTH_NOT_FOUND);
                });
    if (!operation.isOpenAt(LocalTime.now())) {
      log.warn("[OrderService] 주문 가능 시간 외 주문 요청 - 학과명: {}", booth.getDepartment().getDescription());
      throw new CustomException(OrderErrorCode.NOT_TIME_TO_ORDER);
    }
    return operation;
  }

  private void validateBoothMenusOrderable(
      List<BoothMenu> boothMenus, BoothOperation boothOperation) {
    validateBoothMenuTimeType(boothMenus, boothOperation);
    validateBoothMenusSoldOut(boothMenus);
  }

  private void validateBoothMenusSoldOut(List<BoothMenu> boothMenus) {
    boolean hasSoldOut = boothMenus.stream().anyMatch(BoothMenu::getIsSoldOut);
    if (hasSoldOut) {
      log.warn("[OrderService] 품절 메뉴 포함 주문 요청");
      throw new CustomException(OrderErrorCode.ORDER_MENU_SOLD_OUT);
    }
  }

  private void validateBoothMenuTimeType(List<BoothMenu> boothMenus, BoothOperation operation) {
    TimeType currentTimeType = operation.getCurrentOrderTimeType(LocalTime.now());
    List<TimeType> allowedTypes = TimeType.forQuery(currentTimeType);

    boolean hasInvalidMenu =
        boothMenus.stream().anyMatch(menu -> !allowedTypes.contains(menu.getTimeType()));

    if (hasInvalidMenu) {
      log.warn("[OrderService] 현재 시간대에 주문 불가한 메뉴 포함 - 현재 시간 타입: {}", currentTimeType);
      throw new CustomException(OrderErrorCode.ORDER_MENU_TIME_TYPE_NOT_AVAILABLE);
    }
  }
}
