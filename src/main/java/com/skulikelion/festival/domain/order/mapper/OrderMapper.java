/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.mapper;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

import com.skulikelion.festival.domain.order.dto.request.CookingOrderCursor;
import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.entity.BoothMenu;
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
    return WaitingOrderResponse.builder()
        .orderId(order.getId())
        .tableNumber(order.getTableNumber())
        .numOfPeople(order.getNumOfPeople())
        .customerName(order.getCustomerName())
        .customerPhoneNumber(order.getCustomerPhoneNumber())
        .orderTime(order.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")))
        .totalOrderPrice(order.getTotalOrderPrice())
        .orderItems(toWaitingOrderItemResponseList(orderItems))
        .build();
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
    return WaitingOrderResponse.builder()
        .orderId(order.getId())
        .tableNumber(order.getTableNumber())
        .numOfPeople(order.getNumOfPeople())
        .customerName(order.getCustomerName())
        .customerPhoneNumber(order.getCustomerPhoneNumber())
        .orderTime(order.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")))
        .totalOrderPrice(order.getTotalOrderPrice())
        .orderItems(orderItems)
        .build();
  }

  public CookingOrderResponse toCookingOrderResponseFromDto(
      Order order, List<CookingOrderItemUnitResponse> orderItemUnits) {
    return CookingOrderResponse.builder()
        .orderId(order.getId())
        .tableNumber(order.getTableNumber())
        .numOfPeople(order.getNumOfPeople())
        .customerName(order.getCustomerName())
        .customerPhoneNumber(order.getCustomerPhoneNumber())
        .orderTime(order.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")))
        .totalOrderPrice(order.getTotalOrderPrice())
        .orderItemUnits(orderItemUnits)
        .build();
  }

  public CompletedOrderResponse toCompletedOrderResponseFromDto(
      Order order, List<CompletedOrderItemResponse> orderItems) {
    return CompletedOrderResponse.builder()
        .orderId(order.getId())
        .tableNumber(order.getTableNumber())
        .numOfPeople(order.getNumOfPeople())
        .customerName(order.getCustomerName())
        .customerPhoneNumber(order.getCustomerPhoneNumber())
        .orderDate(order.getCreatedAt().format(DateTimeFormatter.ofPattern("M/d")))
        .orderTime(order.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")))
        .completeTime(order.getCompletedAt().format(DateTimeFormatter.ofPattern("HH:mm")))
        .totalOrderPrice(order.getTotalOrderPrice())
        .orderItems(orderItems)
        .build();
  }

  public CanceledOrderResponse toCanceledOrderResponseFromDto(
      Order order,
      List<CanceledOrderItemResponse> orderItems,
      OrderCancelReason orderCancelReason) {
    return CanceledOrderResponse.builder()
        .orderId(order.getId())
        .tableNumber(order.getTableNumber())
        .numOfPeople(order.getNumOfPeople())
        .customerName(order.getCustomerName())
        .customerPhoneNumber(order.getCustomerPhoneNumber())
        .totalOrderPrice(order.getTotalOrderPrice())
        .orderDate(order.getCreatedAt().format(DateTimeFormatter.ofPattern("M/d")))
        .orderTime(order.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")))
        .cancelTime(order.getCanceledAt().format(DateTimeFormatter.ofPattern("HH:mm")))
        .orderCancelReason(orderCancelReason.getDescription())
        .orderItems(orderItems)
        .build();
  }

  public OrderItemUnitStatusResponse toOrderItemUnitStatusResponse(OrderItemUnit orderItemUnit) {
    return OrderItemUnitStatusResponse.builder()
        .orderItemUnitId(orderItemUnit.getId())
        .orderId(orderItemUnit.getOrderItem().getOrder().getId())
        .served(orderItemUnit.isServed())
        .build();
  }

  private <T, C> CursorPageResponse<T> toCursorPageResponse(CursorPage<T> cursorPage, Function<T, C> cursorExtractor) {
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
    return toCursorPageResponse(cursorPage, item -> new WaitingOrderCursor(item.getCreatedAt(), item.getOrderId()));
  }

  public CursorPageResponse<CookingOrderResponse> toCookingOrderResponseCursorPage(CursorPage<CookingOrderResponse> cursorPage) {
    return toCursorPageResponse(cursorPage, item -> new CookingOrderCursor(item.getModifiedAt(), item.getOrderId()));
  }
}
