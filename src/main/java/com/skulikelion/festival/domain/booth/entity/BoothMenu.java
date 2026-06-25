/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.entity;

import jakarta.persistence.*;

import com.skulikelion.festival.domain.booth.enums.MenuCategory;
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
@Table(name = "booth_menu")
public class BoothMenu extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booth_id", nullable = false)
  private Booth booth;

  @Column(nullable = false)
  private String nameKo;

  @Column private String nameEn;

  @Column private String nameZh;

  private Integer price;

  @Enumerated(EnumType.STRING)
  private TimeType timeType;

  @Builder.Default
  @Column(nullable = false)
  private Boolean isSoldOut = false;

  @Column(columnDefinition = "TEXT")
  private String descriptionKo;

  @Column(columnDefinition = "TEXT")
  private String descriptionEn;

  @Column(columnDefinition = "TEXT")
  private String descriptionZh;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MenuCategory category;

  public void update(
      String nameKo,
      Integer price,
      TimeType timeType,
      Boolean isSoldOut,
      String descriptionKo,
      MenuCategory category) {
    this.nameKo = nameKo;
    this.price = price;
    this.timeType = timeType;
    this.isSoldOut = Boolean.TRUE.equals(isSoldOut);
    this.descriptionKo = descriptionKo;
    this.category = category;
  }

  public void updatePrice(Integer price) {
    this.price = price;
  }

  public void updateSoldOut(Boolean isSoldOut) {
    this.isSoldOut = Boolean.TRUE.equals(isSoldOut);
  }
}
