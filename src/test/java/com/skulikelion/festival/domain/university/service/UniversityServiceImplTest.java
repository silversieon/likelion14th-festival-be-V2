/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.university.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skulikelion.festival.domain.university.dto.response.DepartmentResponse;
import com.skulikelion.festival.domain.university.dto.response.UniversitySearchResponse;
import com.skulikelion.festival.domain.university.exception.UniversityErrorCode;
import com.skulikelion.festival.domain.university.repository.DepartmentRepository;
import com.skulikelion.festival.domain.university.repository.UniversityRepository;
import com.skulikelion.festival.global.exception.CustomException;

/**
 * {@link UniversityServiceImpl} 테스트입니다. (LLD-0002 13.1)
 *
 * @since 2026.09.16
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UniversityServiceImpl은")
class UniversityServiceImplTest {

  @Mock private UniversityRepository universityRepository;
  @Mock private DepartmentRepository departmentRepository;

  @InjectMocks private UniversityServiceImpl universityService;

  @Nested
  @DisplayName("searchUniversities는")
  class SearchUniversities {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("검색어가 없거나 공백뿐이면 UNIVERSITY_NAME_REQUIRED를 던진다")
    void throwsNameRequired_whenBlank(String name) {
      assertThatThrownBy(() -> universityService.searchUniversities(name))
          .isInstanceOf(CustomException.class)
          .extracting(e -> ((CustomException) e).getErrorCode())
          .isEqualTo(UniversityErrorCode.UNIVERSITY_NAME_REQUIRED);
    }

    @Test
    @DisplayName("검색어 앞뒤 공백을 제거해 조회하고 결과를 그대로 반환한다")
    void trimsNameAndReturnsResult() {
      List<UniversitySearchResponse> expected = List.of(new UniversitySearchResponse(1L, "서경대학교"));
      given(universityRepository.searchByNamePrefix("서경")).willReturn(expected);

      List<UniversitySearchResponse> result = universityService.searchUniversities("  서경  ");

      assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("검색어의 !, %, _ 앞에 이스케이프 문자 !를 붙여 조회한다")
    void escapesLikeWildcards() {
      given(universityRepository.searchByNamePrefix("100!%!_대!!학")).willReturn(List.of());

      universityService.searchUniversities("100%_대!학");

      verify(universityRepository).searchByNamePrefix("100!%!_대!!학");
    }
  }

  @Nested
  @DisplayName("getDepartments는")
  class GetDepartments {

    @Test
    @DisplayName("학교가 없으면 UNIVERSITY_NOT_FOUND를 던지고 학과를 조회하지 않는다")
    void throwsUniversityNotFound_andSkipsDepartmentQuery() {
      given(universityRepository.existsById(999L)).willReturn(false);

      assertThatThrownBy(() -> universityService.getDepartments(999L))
          .isInstanceOf(CustomException.class)
          .extracting(e -> ((CustomException) e).getErrorCode())
          .isEqualTo(UniversityErrorCode.UNIVERSITY_NOT_FOUND);

      verify(departmentRepository, never()).findAllByUniversityId(anyLong());
    }

    @Test
    @DisplayName("학교가 있으면 그 학교의 학과 목록을 반환한다")
    void returnsDepartmentsOfUniversity() {
      List<DepartmentResponse> expected =
          List.of(new DepartmentResponse(10L, "경영학과"), new DepartmentResponse(11L, "소프트웨어학과"));
      given(universityRepository.existsById(1L)).willReturn(true);
      given(departmentRepository.findAllByUniversityId(1L)).willReturn(expected);

      assertThat(universityService.getDepartments(1L)).isSameAs(expected);
    }
  }
}
