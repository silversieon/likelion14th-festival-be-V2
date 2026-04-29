/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.skulikelion.festival.domain.booth.enums.TimeType;

class BoothOperationTest {

  @Test
  @DisplayName("낮 운영 시작 시간부터 영업중이다")
  void isOpenReturnsTrueWithinDayOperatingTime() {
    BoothOperation operation = createOperation(TimeType.DAY, "13:00", null, "18:00");

    assertThat(operation.isOpenAt(LocalTime.of(13, 0))).isTrue();
    assertThat(operation.isOpenAt(LocalTime.of(17, 59))).isTrue();
  }

  @Test
  @DisplayName("운영 마감 시간부터는 영업중이 아니다")
  void isOpenReturnsFalseAtCloseTime() {
    BoothOperation operation = createOperation(TimeType.DAY, "13:00", null, "18:00");

    assertThat(operation.isOpenAt(LocalTime.of(18, 0))).isFalse();
  }

  @Test
  @DisplayName("밤 운영은 자정을 넘겨도 영업중으로 계산한다")
  void isOpenHandlesOvernightNightOperatingTime() {
    BoothOperation operation = createOperation(TimeType.NIGHT, null, "18:00", "02:00");

    assertThat(operation.isOpenAt(LocalTime.of(23, 0))).isTrue();
    assertThat(operation.isOpenAt(LocalTime.of(1, 0))).isTrue();
    assertThat(operation.isOpenAt(LocalTime.of(3, 0))).isFalse();
  }

  @Test
  @DisplayName("낮밤 운영은 낮 시작 시간부터 마감 전까지 영업중이다")
  void isOpenReturnsTrueForAllDayOperatingTime() {
    BoothOperation operation = createOperation(TimeType.ALL, "13:00", "18:00", "00:00");

    assertThat(operation.isOpenAt(LocalTime.of(13, 0))).isTrue();
    assertThat(operation.isOpenAt(LocalTime.of(23, 0))).isTrue();
  }

  @Test
  @DisplayName("밤 운영 시간 안이면 주문 가능 시간이다")
  void isNightOpenAtReturnsTrueWithinNightTime() {
    BoothOperation operation = createOperation(TimeType.ALL, "13:00", "18:00", "00:00");

    assertThat(operation.isNightOpenAt(LocalTime.of(17, 59))).isFalse();
    assertThat(operation.isNightOpenAt(LocalTime.of(18, 0))).isTrue();
    assertThat(operation.isNightOpenAt(LocalTime.of(23, 59))).isTrue();
  }

  private BoothOperation createOperation(
      TimeType timeType, String dayOpenTime, String nightOpenTime, String closeTime) {
    return BoothOperation.builder()
        .operationDate(LocalDate.of(2026, 5, 7))
        .timeType(timeType)
        .dayOpenTime(dayOpenTime == null ? null : LocalTime.parse(dayOpenTime))
        .nightOpenTime(nightOpenTime == null ? null : LocalTime.parse(nightOpenTime))
        .closeTime(LocalTime.parse(closeTime))
        .build();
  }
}
