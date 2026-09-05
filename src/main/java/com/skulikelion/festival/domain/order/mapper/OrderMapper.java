/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.mapper;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.entity.BoothMenu;
import com.skulikelion.festival.domain.order.dto.request.CanceledOrderCursor;
import com.skulikelion.festival.domain.order.dto.request.CompletedOrderCursor;
import com.skulikelion.festival.domain.order.dto.request.CookingOrderCursor;
import com.skulikelion.festival.domain.order.dto.request.OrderCreateRequest;
import com.skulikelion.festival.domain.order.dto.request.OrderItemCreateRequest;
import com.skulikelion.festival.domain.order.dto.request.WaitingOrderCursor;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderItemUnitResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderItemUnitStatusResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;
import com.skulikelion.festival.domain.order.entity.Order;
import com.skulikelion.festival.domain.order.entity.OrderItem;
import com.skulikelion.festival.domain.order.entity.OrderItemUnit;
import com.skulikelion.festival.domain.order.entity.enums.OrderCancelReason;
import com.skulikelion.festival.global.common.pagenation.CursorCodec;
import com.skulikelion.festival.global.common.pagenation.CursorPage;
import com.skulikelion.festival.global.common.pagenation.CursorPageResponse;
import com.skulikelion.festival.global.enums.Language;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderMapper {

  private final CursorCodec cursorCodec;

  public Order createOrderFromOrderCreateRequest(OrderCreateRequest request) {
    return Order.builder()
        .tableNumber(request.getTableNumber())
        .numOfPeople(request.getNumOfPeople())
        .customerName(request.getCustomerName())
        .customerPhoneNumber(request.getCustomerPhoneNumber())
        .totalOrderPrice(request.getTotalOrderPrice())
        .build();
  }

  public OrderItem createOrderItemFromOrderItemCreateRequest(
      OrderItemCreateRequest request, Order order, BoothMenu boothMenu) {
    return OrderItem.builder()
        .quantity(request.getQuantity())
        .menuPrice(request.getMenuPrice())
        .totalOrderItemPrice(request.getTotalOrderItemPrice())
        .order(order)
        .boothMenu(boothMenu)
        .build();
  }

  public OrderItemUnit createOrderItemUnitFromOrderItem(OrderItem orderItem) {
    return OrderItemUnit.builder().orderItem(orderItem).build();
  }

  public List<OrderItemResponse> toOrderItemResponseList(
      List<OrderItem> orderItems, Language language) {
    return orderItems.stream().map(orderItem -> toOrderItemResponse(orderItem, language)).toList();
  }

  public OrderItemResponse toOrderItemResponse(OrderItem orderItem, Language language) {
    return OrderItemResponse.builder()
        .orderItemId(orderItem.getId())
        .orderId(orderItem.getOrder().getId())
        .menuName(language.getMenuName(orderItem.getBoothMenu()))
        .quantity(orderItem.getQuantity())
        .menuPrice(orderItem.getMenuPrice())
        .totalOrderItemPrice(orderItem.getTotalOrderItemPrice())
        .build();
  }

  public OrderResponse toOrderResponse(
      Order order, Booth booth, List<OrderItemResponse> orderItemResponses) {
    return OrderResponse.builder()
        .orderId(order.getId())
        .customerName(order.getCustomerName())
        .customerPhoneNumber(order.getCustomerPhoneNumber())
        .orderTime(order.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")))
        .orderItems(orderItemResponses)
        .totalOrderPrice(order.getTotalOrderPrice())
        .bankName(booth.getBankName())
        .accountName(booth.getAccountName())
        .accountNumber(booth.getAccountNumber())
        .build();
  }

  public WaitingOrderResponse toWaitingOrderResponse(Order order, List<OrderItem> orderItems) {
    return new WaitingOrderResponse(
        order.getId(),
        order.getTableNumber(),
        order.getNumOfPeople(),
        order.getCustomerName(),
        order.getCustomerPhoneNumber(),
        order.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")),
        order.getTotalOrderPrice(),
        toWaitingOrderItemResponseList(orderItems));
  }

  private List<WaitingOrderItemResponse> toWaitingOrderItemResponseList(
      List<OrderItem> orderItems) {
    return orderItems.stream().map(this::toWaitingOrderItemResponse).toList();
  }

  private WaitingOrderItemResponse toWaitingOrderItemResponse(OrderItem orderItem) {
    return WaitingOrderItemResponse.builder()
        .orderItemId(orderItem.getId())
        .orderId(orderItem.getOrder().getId())
        .menuName(orderItem.getBoothMenu().getNameKo())
        .quantity(orderItem.getQuantity())
        .totalOrderItemPrice(orderItem.getTotalOrderItemPrice())
        .build();
  }

  public WaitingOrderResponse toWaitingOrderResponseFromDto(
      Order order, List<WaitingOrderItemResponse> orderItems) {
    return new WaitingOrderResponse(
        order.getId(),
        order.getTableNumber(),
        order.getNumOfPeople(),
        order.getCustomerName(),
        order.getCustomerPhoneNumber(),
        order.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")),
        order.getTotalOrderPrice(),
        orderItems);
  }

  public CookingOrderResponse toCookingOrderResponseFromDto(
      Order order, List<CookingOrderItemUnitResponse> orderItemUnits) {
    return new CookingOrderResponse(
        order.getId(),
        order.getTableNumber(),
        order.getNumOfPeople(),
        order.getCustomerName(),
        order.getCustomerPhoneNumber(),
        order.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")),
        order.getTotalOrderPrice(),
        orderItemUnits);
  }

  public CompletedOrderResponse toCompletedOrderResponseFromDto(
      Order order, List<CompletedOrderItemResponse> orderItems) {
    return new CompletedOrderResponse(
        order.getId(),
        order.getTableNumber(),
        order.getNumOfPeople(),
        order.getCustomerName(),
        order.getCustomerPhoneNumber(),
        order.getTotalOrderPrice(),
        order.getCreatedAt().format(DateTimeFormatter.ofPattern("M/d")),
        order.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")),
        order.getCompletedAt().format(DateTimeFormatter.ofPattern("HH:mm")),
        orderItems);
  }

  public CanceledOrderResponse toCanceledOrderResponseFromDto(
      Order order,
      List<CanceledOrderItemResponse> orderItems,
      OrderCancelReason orderCancelReason) {
    return new CanceledOrderResponse(
        order.getId(),
        order.getTableNumber(),
        order.getNumOfPeople(),
        order.getCustomerName(),
        order.getCustomerPhoneNumber(),
        order.getTotalOrderPrice(),
        order.getCreatedAt().format(DateTimeFormatter.ofPattern("M/d")),
        order.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")),
        order.getCanceledAt().format(DateTimeFormatter.ofPattern("HH:mm")),
        orderCancelReason.getDescription(),
        orderItems);
  }

  public OrderItemUnitStatusResponse toOrderItemUnitStatusResponse(OrderItemUnit orderItemUnit) {
    return OrderItemUnitStatusResponse.builder()
        .orderItemUnitId(orderItemUnit.getId())
        .orderId(orderItemUnit.getOrderItem().getOrder().getId())
        .served(orderItemUnit.isServed())
        .build();
  }

  private <T, C> CursorPageResponse<T> toCursorPageResponse(
      CursorPage<T> cursorPage, Function<T, C> cursorExtractor) {
    List<T> content = cursorPage.content();
    if (content == null || content.isEmpty()) {
      return CursorPageResponse.of(content, null, false, 0);
    }

    T last = content.getLast();
    C nextCursor = cursorExtractor.apply(last);

    return CursorPageResponse.of(
        content,
        cursorPage.hasNext() ? cursorCodec.encode(nextCursor) : null,
        cursorPage.hasNext(),
        cursorPage.content().size());
  }

  public CursorPageResponse<WaitingOrderResponse> toWaitingOrderResponseCursorPage(
      CursorPage<WaitingOrderResponse> cursorPage) {
    return toCursorPageResponse(
        cursorPage, item -> new WaitingOrderCursor(item.getCreatedAt(), item.getOrderId()));
  }

  public CursorPageResponse<CookingOrderResponse> toCookingOrderResponseCursorPage(
      CursorPage<CookingOrderResponse> cursorPage) {
    return toCursorPageResponse(
        cursorPage, item -> new CookingOrderCursor(item.getModifiedAt(), item.getOrderId()));
  }

  public CursorPageResponse<CompletedOrderResponse> toCompletedOrderResponseCursorPage(
      CursorPage<CompletedOrderResponse> cursorPage) {
    return toCursorPageResponse(
        cursorPage, item -> new CompletedOrderCursor(item.getCompletedAt(), item.getOrderId()));
  }

  public CursorPageResponse<CanceledOrderResponse> toCanceledOrderResponseCursorPage(
      CursorPage<CanceledOrderResponse> cursorPage) {
    return toCursorPageResponse(
        cursorPage, item -> new CanceledOrderCursor(item.getCanceledAt(), item.getOrderId()));
  }
}
