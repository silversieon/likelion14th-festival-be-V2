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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.skulikelion.festival.domain.booth.converter.IntegerListJsonConverter;
import com.skulikelion.festival.domain.booth.enums.BoothLocation;
import com.skulikelion.festival.domain.booth.enums.BoothStatus;
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.domain.university.entity.Department;
import com.skulikelion.festival.global.common.BaseTimeEntity;
import com.skulikelion.festival.global.exception.CustomException;

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

  /**
   * 이 부스를 운영하는 학과입니다. {@code UNIQUE(department_id)}가 곧 1:1 관계를 강제한다.
   *
   * <p>전국 확장 이전에는 {@code Department} enum 값을 가진 {@code VARCHAR(100)} UNIQUE 컬럼이었다. 대학이 여러 개가 되면 동명
   * 학과가 그 제약을 깨뜨리므로 학과 테이블 참조로 바꿨다. (ADR-0001)
   */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id", nullable = false, unique = true)
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

  public void validateOrderable() {
    if (!orderEnabled) throw new CustomException(BoothErrorCode.BOOTH_NOT_USING_ORDER);
    if (boothStatus != BoothStatus.OPEN) throw new CustomException(BoothErrorCode.BOOTH_NOT_OPEN);
  }
}
