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
@Table(name = "booth_detail_ch")
public class BoothDetailCh extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long boothChId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booth_id")
  private Booth booth;

  @Column(name = "booth_faculty_ch", nullable = false, unique = true)
  private String boothFacultyCh;

  @Column(name = "booth_title_ch", nullable = false)
  private String boothTitleCh;

  @Column(name = "booth_description_ch", columnDefinition = "TEXT", nullable = false)
  private String boothDescriptionCh;

  @Column(name = "booth_location_ch", nullable = false)
  private String boothLocationCh;

  public void update(
      Booth booth, String faculty, String title, String description, String location) {
    this.booth = booth;
    this.boothFacultyCh = faculty;
    this.boothTitleCh = title;
    this.boothDescriptionCh = description;
    this.boothLocationCh = location;
  }
}
