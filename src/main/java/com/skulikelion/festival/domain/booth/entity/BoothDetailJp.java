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
@Table(name = "booth_detail_jp")
public class BoothDetailJp extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long boothJpId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booth_id")
  private Booth booth;

  @Column(name = "booth_faculty_jp", nullable = false, unique = true)
  private String boothFacultyJp;

  @Column(name = "booth_title_jp", nullable = false)
  private String boothTitleJp;

  @Column(name = "booth_description_jp", columnDefinition = "TEXT", nullable = false)
  private String boothDescriptionJp;

  @Column(name = "booth_location_jp", nullable = false)
  private String boothLocationJp;

  public void update(
      Booth booth, String faculty, String title, String description, String location) {
    this.booth = booth;
    this.boothFacultyJp = faculty;
    this.boothTitleJp = title;
    this.boothDescriptionJp = description;
    this.boothLocationJp = location;
  }
}
