/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.entity;

import java.time.LocalDate;

import jakarta.persistence.*;

import com.skulikelion.festival.global.common.BaseTimeEntity;

import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lostitem")
public class LostItem extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name; // 분실물 이름

  @Column(length = 1000, nullable = true)
  private String imageUrl; // 분실물 이미지 url

  @Column(nullable = false)
  private String foundPlace; // 습득 장소

  @Column(nullable = false)
  private LocalDate foundDate; // 습득 날짜

  @Column(nullable = false, columnDefinition = "tinyint(1) default 0")
  private boolean isReturned = false; // 수령 유무, true면 찾아감, false면 못찾음, default는 false

  @Column(nullable = false, columnDefinition = "tinyint(1) default 0")
  private boolean isDeleted = false; // soft delete 구현
}
