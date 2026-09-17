/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.university.entity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.skulikelion.festival.domain.university.enums.Region;
import com.skulikelion.festival.domain.university.exception.UniversityErrorCode;
import com.skulikelion.festival.global.exception.CustomException;

/**
 * {@link Department}의 불변식 검증 테스트입니다. (LLD-0001 13.1 A그룹)
 *
 * @since 2026.09.14
 */
@DisplayName("Department 엔티티")
class DepartmentTest {

  private static University university(Long id, String name) {
    return University.builder().id(id).name(name).region(Region.SEOUL).build();
  }

  private static Department department(University university, String name) {
    return Department.builder().id(100L).university(university).name(name).build();
  }

  @Nested
  @DisplayName("validateBelongsTo는")
  class ValidateBelongsTo {

    @Test
    @DisplayName("소속 대학 식별자와 같으면 예외를 던지지 않는다")
    void doesNotThrow_whenUniversityMatches() {
      Department department = department(university(1L, "서경대학교"), "소프트웨어학과");

      assertThatCode(() -> department.validateBelongsTo(1L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("소속 대학 식별자와 다르면 DEPARTMENT_NOT_IN_UNIVERSITY 예외를 던진다")
    void throwsDepartmentNotInUniversity_whenUniversityDiffers() {
      Department department = department(university(1L, "서경대학교"), "소프트웨어학과");

      assertThatThrownBy(() -> department.validateBelongsTo(2L))
          .isInstanceOf(CustomException.class)
          .extracting(e -> ((CustomException) e).getErrorCode())
          .isEqualTo(UniversityErrorCode.DEPARTMENT_NOT_IN_UNIVERSITY);
    }
  }
}
