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
@Table(name = "booth_detail_en")
public class BoothDetailEn extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long boothEnId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booth_id")
  private Booth booth;

  @Column(name = "booth_faculty_en", nullable = false, unique = true)
  private String boothFacultyEn;

  @Column(name = "booth_title_en", nullable = false)
  private String boothTitleEn;

  @Column(name = "booth_description_en", columnDefinition = "TEXT", nullable = false)
  private String boothDescriptionEn;

  @Column(name = "booth_location_en", nullable = false)
  private String boothLocationEn;

  public void update(
      Booth booth, String faculty, String title, String description, String location) {
    this.booth = booth;
    this.boothFacultyEn = faculty;
    this.boothTitleEn = title;
    this.boothDescriptionEn = description;
    this.boothLocationEn = location;
  }
}
