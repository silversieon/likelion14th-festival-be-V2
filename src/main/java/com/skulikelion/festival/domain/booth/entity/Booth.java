/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.skulikelion.festival.domain.booth.enums.BoothLocation;
import com.skulikelion.festival.global.common.BaseTimeEntity;
import com.skulikelion.festival.global.enums.Department;

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
@Table(name = "booth")
public class Booth extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Department department;

  private String name; // 삭제 예정 필드

  private String thumbnailUrl;

  private String instagramUrl;

  private LocalDateTime openTime; // 부스 영업중 표시 시작
  private LocalDateTime orderOpenTime; // null이면 주문 서비스 미사용, 있으면 주문 버튼 표시 시작
  private LocalDateTime closeTime;

  @Enumerated(EnumType.STRING)
  private BoothLocation location;

  private String locationDetail;

  private String accountName;
  private String accountNumber;
  private String bankName;

  public void update(
      Department department,
      String thumbnailUrl,
      String instagramUrl,
      LocalDateTime openTime,
      LocalDateTime pubOpenTime,
      LocalDateTime closeTime,
      BoothLocation location,
      String locationDetail,
      String accountName,
      String accountNumber,
      String bankName) {
    this.department = department;
    this.thumbnailUrl = thumbnailUrl;
    this.instagramUrl = instagramUrl;
    this.openTime = openTime;
    this.orderOpenTime = pubOpenTime;
    this.closeTime = closeTime;
    this.location = location;
    this.locationDetail = locationDetail;
    this.accountName = accountName;
    this.accountNumber = accountNumber;
    this.bankName = bankName;
  }

  public boolean isOrderEnabled() {
    return orderOpenTime != null;
  }

  public boolean isOrderAvailable(LocalDateTime now) {
    return isOrderEnabled()
        && closeTime != null
        && !now.isBefore(orderOpenTime)
        && now.isBefore(closeTime);
  }
}
