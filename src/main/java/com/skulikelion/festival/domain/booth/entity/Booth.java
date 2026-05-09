/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.skulikelion.festival.domain.booth.converter.IntegerListJsonConverter;
import com.skulikelion.festival.domain.booth.enums.BoothLocation;
import com.skulikelion.festival.domain.booth.enums.BoothStatus;
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

  @Builder.Default
  @Enumerated(EnumType.STRING)
  private BoothStatus boothStatus = BoothStatus.CLOSED;

  @Enumerated(EnumType.STRING)
  private BoothLocation location;

  @Convert(converter = IntegerListJsonConverter.class)
  @Column(columnDefinition = "JSON")
  private List<Integer> boothNumbers;

  private String accountName;
  private String accountNumber;
  private String bankName;

  public void update(
      Department department,
      String thumbnailUrl,
      Boolean orderEnabled,
      BoothLocation location,
      List<Integer> boothNumbers,
      String accountName,
      String accountNumber,
      String bankName) {
    this.department = department;
    this.thumbnailUrl = thumbnailUrl;
    this.orderEnabled = Boolean.TRUE.equals(orderEnabled);
    this.location = location;
    this.boothNumbers = boothNumbers;
    this.accountName = accountName;
    this.accountNumber = accountNumber;
    this.bankName = bankName;
  }

  public void updateThumbnailUrl(String thumbnailUrl) {
    this.thumbnailUrl = thumbnailUrl;
  }

  public boolean isOrderEnabled() {
    return Boolean.TRUE.equals(orderEnabled);
  }

  public void changeStatus(BoothStatus newStatus) {
    this.boothStatus = newStatus;
  }
}
