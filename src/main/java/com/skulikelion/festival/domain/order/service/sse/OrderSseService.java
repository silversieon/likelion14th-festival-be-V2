/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderItemUnitResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;

public interface OrderSseService {

  SseEmitter subscribeOrder(Long boothId, OrderStatus orderStatus);

  void sendOrderEventNotification(Booth booth, OrderStatus currentStatus);

  void sendWaitingOrderEvent(Booth booth, WaitingOrderResponse waitingOrderResponse);

  void sendCookingOrderEvent(Booth booth, CookingOrderResponse cookingOrderResponse);

  void sendCompletedOrderEvent(Booth booth, CompletedOrderResponse completedOrderResponse);

  void sendCanceledOrderEvent(Booth booth, CanceledOrderResponse canceledOrderResponse);

  void sendCookingOrderItemUnitEvent(
      Booth booth, CookingOrderItemUnitResponse cookingOrderItemUnitResponse);
}
