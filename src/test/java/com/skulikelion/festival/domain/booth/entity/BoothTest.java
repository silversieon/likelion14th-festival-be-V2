/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BoothTest {

  @Test
  @DisplayName("영업 시간 안이면 영업중이다")
  void isOpenReturnsTrueWithinSameDayOperatingTime() {
    Booth booth = createBooth("13:00", "17:00", "22:00");

    assertThat(booth.isOpen(LocalTime.of(13, 0))).isTrue();
    assertThat(booth.isOpen(LocalTime.of(18, 0))).isTrue();
  }

  @Test
  @DisplayName("영업 종료 시각부터는 영업중이 아니다")
  void isOpenReturnsFalseAtCloseTime() {
    Booth booth = createBooth("13:00", "17:00", "22:00");

    assertThat(booth.isOpen(LocalTime.of(22, 0))).isFalse();
  }

  @Test
  @DisplayName("자정을 넘기는 영업 시간도 영업중으로 계산한다")
  void isOpenHandlesOvernightOperatingTime() {
    Booth booth = createBooth("18:00", "18:30", "02:00");

    assertThat(booth.isOpen(LocalTime.of(23, 0))).isTrue();
    assertThat(booth.isOpen(LocalTime.of(1, 0))).isTrue();
    assertThat(booth.isOpen(LocalTime.of(3, 0))).isFalse();
  }

  @Test
  @DisplayName("주문 시작 전에는 주문 버튼이 비활성화된다")
  void isOrderAvailableReturnsFalseBeforeOrderOpenTime() {
    Booth booth = createBooth("13:00", "17:00", "22:00");

    assertThat(booth.isOrderAvailable(LocalTime.of(16, 59))).isFalse();
  }

  @Test
  @DisplayName("주문 가능 시간 안이면 주문 버튼이 활성화된다")
  void isOrderAvailableReturnsTrueWithinOrderTime() {
    Booth booth = createBooth("13:00", "17:00", "22:00");

    assertThat(booth.isOrderAvailable(LocalTime.of(17, 0))).isTrue();
    assertThat(booth.isOrderAvailable(LocalTime.of(21, 59))).isTrue();
  }

  @Test
  @DisplayName("주문 시작 시간이 없으면 주문 버튼이 비활성화된다")
  void isOrderAvailableReturnsFalseWhenOrderOpenTimeIsNull() {
    Booth booth =
        Booth.builder().openTime(LocalTime.of(13, 0)).closeTime(LocalTime.of(22, 0)).build();

    assertThat(booth.isOrderEnabled()).isFalse();
    assertThat(booth.isOrderAvailable(LocalTime.of(18, 0))).isFalse();
  }

  private Booth createBooth(String openTime, String orderOpenTime, String closeTime) {
    return Booth.builder()
        .openTime(LocalTime.parse(openTime))
        .orderOpenTime(LocalTime.parse(orderOpenTime))
        .closeTime(LocalTime.parse(closeTime))
        .build();
  }
}
