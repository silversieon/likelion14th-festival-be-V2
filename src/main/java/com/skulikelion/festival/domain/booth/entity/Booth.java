/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.skulikelion.festival.global.common.BaseTimeEntity;

import lombok.*;

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

  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @Column(name = "password")
  private String password;

  @Column(name = "waiting_team", nullable = false)
  private Integer waitingTeam;

  @Column(name = "opening_hours", nullable = false)
  private OpeningHours openingHours;

  @OneToMany(mappedBy = "booth", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BoothImage> boothImages = new ArrayList<>();

  @Column(name = "booth_thumbnail_url")
  private String boothThumbnailUrl;

  @Column(name = "booth_instagram")
  private String boothInstagram;

  @Column(name = "service_agreement")
  private Boolean serviceAgreement;

  @OneToMany(mappedBy = "booth", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BoothMenu> boothMenus;

  public void update(
      String name, String password, OpeningHours openingHours, String boothInstagram) {
    this.name = name;
    this.password = password;
    this.openingHours = openingHours;
    this.boothInstagram = boothInstagram;
  }

  public void decreaseWaitingTeam() {
    this.waitingTeam = Math.max(0, this.waitingTeam - 1);
  }

  public void addWaitingTeam() {
    this.waitingTeam += 1;
  }
}
