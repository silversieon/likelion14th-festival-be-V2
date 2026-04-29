/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.skulikelion.festival.domain.booth.enums.TimeType;
import com.skulikelion.festival.global.common.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
    name = "booth_operation",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"booth_id", "operation_date"})})
public class BoothOperation extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booth_id", nullable = false)
  private Booth booth;

  @Column(nullable = false)
  private LocalDate operationDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TimeType timeType;

  @Column private LocalTime dayOpenTime;

  @Column private LocalTime nightOpenTime;

  @Column(nullable = false)
  private LocalTime closeTime;

  public boolean isOpenAt(LocalTime now) {
    return switch (timeType) {
      case DAY, ALL -> dayOpenTime != null && isWithinOperatingTime(now, dayOpenTime, closeTime);
      case NIGHT -> nightOpenTime != null && isWithinOperatingTime(now, nightOpenTime, closeTime);
    };
  }

  public boolean isNightOpenAt(LocalTime now) {
    return switch (timeType) {
      case DAY -> false;
      case NIGHT, ALL -> nightOpenTime != null
          && isWithinOperatingTime(now, nightOpenTime, closeTime);
    };
  }

  private boolean isWithinOperatingTime(LocalTime now, LocalTime startTime, LocalTime endTime) {
    if (startTime.equals(endTime)) {
      return true;
    }
    if (startTime.isBefore(endTime)) {
      return !now.isBefore(startTime) && now.isBefore(endTime);
    }
    return !now.isBefore(startTime) || now.isBefore(endTime);
  }

  public TimeType getCurrentOrderTimeType(LocalTime now) {
    if (nightOpenTime != null && isWithinOperatingTime(now, nightOpenTime, closeTime)) {
      return TimeType.NIGHT;
    }
    if (dayOpenTime != null && isWithinOperatingTime(now, dayOpenTime, closeTime)) {
      return TimeType.DAY;
    }
    return null;
  }
}
