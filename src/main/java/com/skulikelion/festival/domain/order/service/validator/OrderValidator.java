/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service.validator;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.booth.entity.BoothMenu;
import com.skulikelion.festival.domain.booth.entity.BoothOperation;
import com.skulikelion.festival.domain.booth.enums.TimeType;
import com.skulikelion.festival.domain.order.dto.request.OrderItemCreateRequest;
import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.global.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderValidator {

  public void validateMenuPrice(
      List<OrderItemCreateRequest> orderItems, Map<Long, BoothMenu> boothMenuMap) {
    orderItems.forEach(
        item -> {
          BoothMenu boothMenu = boothMenuMap.get(item.getBoothMenuId());
          if (!Objects.equals(boothMenu.getPrice(), item.getMenuPrice())) {
            log.warn(
                "[OrderService] 주문한 메뉴 가격이 실제 가격과 일치하지 않습니다 - 주문 메뉴 가격: {}, 실제 메뉴 가격: {}",
                item.getMenuPrice(),
                boothMenu.getPrice());
            throw new CustomException(OrderErrorCode.ORDER_MENU_PRICE_MISMATCH);
          }
        });
  }

  public void validateBoothMenusOrderable(
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
    TimeType currentTimeType = operation.getOrderableTimeType(LocalTime.now());
    List<TimeType> allowedTypes = TimeType.forQuery(currentTimeType);

    boolean hasInvalidMenu =
        boothMenus.stream().anyMatch(menu -> !allowedTypes.contains(menu.getTimeType()));

    if (hasInvalidMenu) {
      log.warn("[OrderService] 현재 시간대에 주문 불가한 메뉴 포함 - 현재 시간 타입: {}", currentTimeType);
      throw new CustomException(OrderErrorCode.ORDER_MENU_TIME_TYPE_NOT_AVAILABLE);
    }
  }
}
