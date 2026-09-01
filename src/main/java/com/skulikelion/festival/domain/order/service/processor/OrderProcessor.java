/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.processor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.entity.BoothMenu;
import com.skulikelion.festival.domain.booth.entity.BoothOperation;
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.domain.booth.repository.BoothMenuRepository;
import com.skulikelion.festival.domain.booth.repository.BoothOperationRepository;
import com.skulikelion.festival.domain.booth.repository.BoothRepository;
import com.skulikelion.festival.domain.order.dto.payload.WaitingOrderPayload;
import com.skulikelion.festival.domain.order.dto.request.OrderCreateRequest;
import com.skulikelion.festival.domain.order.dto.request.OrderItemCreateRequest;
import com.skulikelion.festival.domain.order.dto.response.OrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;
import com.skulikelion.festival.domain.order.entity.Order;
import com.skulikelion.festival.domain.order.entity.OrderItem;
import com.skulikelion.festival.domain.order.entity.OrderItemUnit;
import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.domain.order.mapper.OrderEventMapper;
import com.skulikelion.festival.domain.order.mapper.OrderMapper;
import com.skulikelion.festival.domain.order.repository.OrderItemRepository;
import com.skulikelion.festival.domain.order.repository.OrderItemUnitRepository;
import com.skulikelion.festival.domain.order.repository.OrderRepository;
import com.skulikelion.festival.domain.order.service.validator.OrderValidator;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.exception.GlobalErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProcessor {

  private final BoothRepository boothRepository;
  private final BoothMenuRepository boothMenuRepository;
  private final OrderMapper orderMapper;
  private final OrderItemRepository orderItemRepository;
  private final OrderItemUnitRepository orderItemUnitRepository;
  private final OrderEventMapper orderEventMapper;
  private final ApplicationEventPublisher eventPublisher;
  private final BoothOperationRepository boothOperationRepository;
  private final OrderRepository orderRepository;
  private final OrderValidator orderValidator;

  @Transactional
  public OrderResponse processOrder(Long boothId, OrderCreateRequest request) {
    Booth booth =
        boothRepository
            .findById(boothId)
            .orElseThrow(
                () -> {
                  log.warn("[OrderService] 부스를 찾을 수 없습니다 - 부스 식별자: {}", boothId);
                  return new CustomException(BoothErrorCode.BOOTH_NOT_FOUND);
                });
    booth.validateOrderable();

    List<Long> boothMenuIds =
        request.getOrderItems().stream().map(OrderItemCreateRequest::getBoothMenuId).toList();
    List<BoothMenu> boothMenus = getBoothMenus(boothMenuIds, boothMenuIds.size());

    LocalDate today = LocalDate.now();
    BoothOperation boothOperation =
        boothOperationRepository
            .findByBoothIdAndOperationDate(boothId, today)
            .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_TIME_BOOTH_NOT_FOUND));
    orderValidator.validateBoothMenusOrderable(boothMenus, boothOperation);

    Map<Long, BoothMenu> boothMenuMap =
        boothMenus.stream().collect(Collectors.toMap(BoothMenu::getId, Function.identity()));
    orderValidator.validateMenuPrice(request.getOrderItems(), boothMenuMap);

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

    log.info(
        "[OrderService] 주문 생성 성공 - 주문 식별자: {}, 학과명: {}, 주문자명: {}, 총 주문 금액: {}",
        order.getId(),
        booth.getDepartment().getDescription(),
        order.getCustomerName(),
        order.getTotalOrderPrice());

    return orderResponse;
  }

  private List<BoothMenu> getBoothMenus(List<Long> boothMenuIds, int boothMenuIdsSize) {
    List<BoothMenu> boothMenus = boothMenuRepository.findAllById(boothMenuIds);
    int boothMenusSize = boothMenus.size();
    if (boothMenusSize != boothMenuIdsSize) {
      log.warn(
          "[OrderService] 존재하지 않는 부스 메뉴 포함 - 요청 메뉴 수: {}, 실제 메뉴 수: {}",
          boothMenuIdsSize,
          boothMenusSize);
      throw new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND);
    }
    return boothMenus;
  }
}
