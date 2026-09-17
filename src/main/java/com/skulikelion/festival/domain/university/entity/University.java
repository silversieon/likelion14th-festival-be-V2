/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.university.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.skulikelion.festival.domain.university.enums.Region;
import com.skulikelion.festival.global.common.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대학입니다. 학과({@link Department})를 1:N으로 거느린다.
 *
 * <p>전국 확장 이전에는 이 개념이 존재하지 않았고, 학과가 {@code global/enums/Department} Java enum으로 하드코딩되어 있었다.
 * (ADR-0001)
 *
 * @since 2026.09.14
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
    name = "universities",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_universities_name_region",
            columnNames = {"name", "region"}))
public class University extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private Region region;
}
