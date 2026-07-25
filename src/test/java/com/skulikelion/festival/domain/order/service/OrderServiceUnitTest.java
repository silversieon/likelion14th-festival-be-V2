/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skulikelion.festival.domain.order.dto.request.OrderCreateRequest;
import com.skulikelion.festival.domain.order.dto.request.OrderItemCreateRequest;
import com.skulikelion.festival.domain.order.dto.response.OrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.OrderResponse;
import com.skulikelion.festival.domain.order.service.idempotency.OrderIdempotencyService;
import com.skulikelion.festival.global.enums.Language;

@ExtendWith(MockitoExtension.class)
public class OrderServiceUnitTest {

  @Mock OrderIdempotencyService orderIdempotencyService;

  @InjectMocks OrderServiceImpl orderService;

  @Test
  @DisplayName("주문 생성 시에 멱등성 서비스에 위임한다.")
  void createOrderDelegatesIdempotency() {
    // given
    Long boothId = 1L;
    String idempotencyKey = "idempotency123";
    OrderItemCreateRequest orderItemCreateRequest = new OrderItemCreateRequest(1L, 2, 12000, 24000);
    OrderCreateRequest request =
        new OrderCreateRequest(
            1, 1, "홍길동", "010-1234-5678", 34000, Language.KO, List.of(orderItemCreateRequest));
    OrderItemResponse orderItemResponse = new OrderItemResponse(1L, 1L, "치킨", 2, 12000, 24000);
    OrderResponse expected =
        new OrderResponse(
            1L,
            "홍길동",
            "010-1234-5678",
            "20:00:00",
            List.of(orderItemResponse),
            24000,
            "우리은행",
            "김길동",
            "111111111");

    // when
    when(orderIdempotencyService.executeIdempotent(
            eq(idempotencyKey), any(), eq(OrderResponse.class)))
        .thenReturn(expected);

    OrderResponse result = orderService.createOrder(boothId, idempotencyKey, request);

    // then
    assertThat(result).isEqualTo(expected);
    verify(orderIdempotencyService)
        .executeIdempotent(eq(idempotencyKey), any(), eq(OrderResponse.class));
  }
}
