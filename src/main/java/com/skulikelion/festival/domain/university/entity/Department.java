/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.university.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.skulikelion.festival.domain.university.exception.UniversityErrorCode;
import com.skulikelion.festival.global.common.BaseTimeEntity;
import com.skulikelion.festival.global.exception.CustomException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 학과입니다. 대학({@link University})에 N:1로 속하며, 부스와는 1:1로 이어진다.
 *
 * <p>{@code UNIQUE(university_id, name)} 덕분에 <b>서로 다른 대학에 같은 이름의 학과가 공존</b>할 수 있다. 이것이 전국 확장의 핵심
 * 제약이다. (ADR-0001)
 *
 * @since 2026.09.14
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
    name = "departments",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_departments_university_name",
            columnNames = {"university_id", "name"}))
public class Department extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "university_id", nullable = false)
  private University university;

  @Column(nullable = false, length = 100)
  private String name;

  /**
   * [ 학과가 해당 대학에 속하는지 검증하는 메서드 ]
   *
   * <p>로그인·회원가입은 대학과 학과를 각각 식별자로 받는다. 둘의 소속 관계를 서비스가 아니라 엔티티가 강제해, 호출 경로마다 검증이 빠지는 일을 막는다.
   *
   * @param universityId 요청에 담겨 온 대학 식별자
   * @throws com.skulikelion.festival.global.exception.CustomException 소속 대학이 다를 경우
   */
  public void validateBelongsTo(Long universityId) {
    if (!university.getId().equals(universityId)) {
      throw new CustomException(UniversityErrorCode.DEPARTMENT_NOT_IN_UNIVERSITY);
    }
  }
}
