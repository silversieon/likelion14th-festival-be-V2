/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.mapper;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.entity.BoothMenu;
import com.skulikelion.festival.domain.order.dto.request.OrderCreateRequest;
import com.skulikelion.festival.domain.order.dto.request.OrderItemCreateRequest;
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

@Component
public class OrderMapper {

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

  public List<OrderItemResponse> toOrderItemResponseList(List<OrderItem> orderItems) {
    return orderItems.stream().map(this::toOrderItemResponse).toList();
  }

  public OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
    return OrderItemResponse.builder()
        .orderItemId(orderItem.getId())
        .orderId(orderItem.getOrder().getId())
        .menuName(orderItem.getBoothMenu().getNameKo())
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

  public CookingOrderResponse toCookingOrderResponse(
      Order order, List<CookingOrderItemResponse> orderItems) {
    return CookingOrderResponse.builder()
        .orderId(order.getId())
        .customerName(order.getCustomerName())
        .customerPhoneNumber(order.getCustomerPhoneNumber())
        .orderTime(order.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")))
        .totalOrderPrice(order.getTotalOrderPrice())
        .orderItems(orderItems)
        .build();
  }

  public CompletedOrderResponse toCompletedOrderResponse(
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

  public CanceledOrderResponse toCanceledOrderResponse(
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

  public CookingOrderItemUnitResponse toCookingOrderItemUnitResponse(OrderItemUnit orderItemUnit) {
    return CookingOrderItemUnitResponse.builder()
        .orderItemUnitId(orderItemUnit.getId())
        .orderItemId(orderItemUnit.getOrderItem().getId())
        .served(orderItemUnit.isServed())
        .build();
  }
}
