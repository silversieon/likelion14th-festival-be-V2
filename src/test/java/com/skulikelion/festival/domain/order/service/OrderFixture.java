/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service;

import java.util.List;

import com.skulikelion.festival.domain.order.dto.response.OrderResponse;

public class OrderFixture {

  public static OrderResponse dummyResponse() {
    return new OrderResponse(
        1L, "홍길동", "010-1234-5678", "20:00:00", List.of(), 24000, "우리은행", "김길동", "111111111");
  }
}
