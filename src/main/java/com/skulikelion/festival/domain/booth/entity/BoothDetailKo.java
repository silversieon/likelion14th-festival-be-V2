package com.skulikelion.festival.domain.booth.entity;

import com.skulikelion.festival.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "booth_detail_ko")
public class BoothDetailKo extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long boothKoId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booth_id")
  private Booth booth;

  @Column(name = "booth_faculty_ko", nullable = false, unique = true)
  private String boothFacultyKo;

  @Column(name = "booth_title_ko", nullable = false)
  private String boothTitleKo;

  @Column(name = "booth_description_ko", columnDefinition = "TEXT", nullable = false)
  private String boothDescriptionKo;

  @Column(name = "booth_location_ko", nullable = false)
  private String boothLocationKo;

  public void update(
      Booth booth, String faculty, String title, String description, String location) {
    this.booth = booth;
    this.boothFacultyKo = faculty;
    this.boothTitleKo = title;
    this.boothDescriptionKo = description;
    this.boothLocationKo = location;
  }
}
