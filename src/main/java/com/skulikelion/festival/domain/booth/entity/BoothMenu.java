/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.entity;

import jakarta.persistence.*;

import com.skulikelion.festival.global.common.BaseTimeEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "booth_menu")
public class BoothMenu extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long menuId;

  @Column(name = "menu_ko", nullable = false)
  private String menuKo;

  @Column(name = "menu_en", nullable = false)
  private String menuEn;

  @Column(name = "menu_ch", nullable = false)
  private String menuCh;

  @Column(name = "menu_jp", nullable = false)
  private String menuJp;

  @Column(name = "menu_price", nullable = false)
  private Integer menuPrice;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booth_id")
  private Booth booth;

  @Enumerated(EnumType.STRING)
  @Column(name = "menu_time_type", nullable = false)
  private OpeningHours menuTimeType;

  public void update(
      Booth booth,
      String menuKo,
      String menuEn,
      String menuCh,
      String menuJp,
      Integer menuPrice,
      OpeningHours menuTimeType) {
    this.booth = booth;
    this.menuKo = menuKo;
    this.menuEn = menuEn;
    this.menuCh = menuCh;
    this.menuJp = menuJp;
    this.menuPrice = menuPrice;
    this.menuTimeType = menuTimeType;
  }
}
