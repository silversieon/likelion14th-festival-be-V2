/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service;

import java.time.LocalDate;
import java.util.List;

import com.skulikelion.festival.domain.order.dto.request.OrderCreateRequest;
import com.skulikelion.festival.domain.order.dto.request.OrderItemUnitUpdateRequest;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;
import com.skulikelion.festival.domain.order.entity.enums.OrderCancelReason;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;

public interface OrderService {

  OrderResponse createOrder(Long boothId, OrderCreateRequest request);

  List<WaitingOrderResponse> getWaitingOrders(String username, Long boothId);

  List<CookingOrderResponse> getCookingOrders(String username, Long boothId);

  List<CompletedOrderResponse> getCompletedOrders(
      String username, Long boothId, LocalDate orderDate, String keyword);

  List<CanceledOrderResponse> getCanceledOrders(
      String username, Long boothId, LocalDate orderDate, String keyword);

  void updateOrderStatus(String username, Long boothId, Long orderId, OrderStatus orderStatus);

  void cancelOrder(
      String username, Long boothId, Long orderId, OrderCancelReason orderCancelReason);

  void updateServedStatus(
      String username, Long boothId, Long orderItemUnitId, OrderItemUnitUpdateRequest request);
}
