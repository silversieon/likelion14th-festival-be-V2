/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.entity;

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
  @Column(nullable = false, unique = true)
  private Department department;

  private String thumbnailUrl;

  @Builder.Default
  @Column(nullable = false)
  private Boolean orderEnabled = false;

  @Enumerated(EnumType.STRING)
  private BoothLocation location;

  private String locationDetail;

  private String accountName;
  private String accountNumber;
  private String bankName;

  public void update(
      Department department,
      String thumbnailUrl,
      Boolean orderEnabled,
      BoothLocation location,
      String locationDetail,
      String accountName,
      String accountNumber,
      String bankName) {
    this.department = department;
    this.thumbnailUrl = thumbnailUrl;
    this.orderEnabled = orderEnabled;
    this.location = location;
    this.locationDetail = locationDetail;
    this.accountName = accountName;
    this.accountNumber = accountNumber;
    this.bankName = bankName;
  }

  public boolean isOrderEnabled() {
    return Boolean.TRUE.equals(orderEnabled);
  }
}
