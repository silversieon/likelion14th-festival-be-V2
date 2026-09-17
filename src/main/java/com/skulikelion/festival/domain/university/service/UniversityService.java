/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.university.service;

import java.util.List;

import com.skulikelion.festival.domain.university.dto.response.DepartmentResponse;
import com.skulikelion.festival.domain.university.dto.response.UniversitySearchResponse;

/**
 * 대학·학과 조회 Service interface 입니다.
 *
 * <p>로그인·회원가입 화면에서 학교를 검색하고 학과를 고르는 흐름에 쓰인다. (LLD-0002)
 *
 * @see com.skulikelion.festival.domain.university.controller.UniversityController
 * @since 2026.09.16
 */
public interface UniversityService {

  /**
   * [ 학교명 전방 일치 검색 메서드 ]
   *
   * @param name 검색할 학교명 (앞부분부터 일치)
   * @return 학교명 오름차순 목록
   */
  List<UniversitySearchResponse> searchUniversities(String name);

  /**
   * [ 학교별 학과 목록 조회 메서드 ]
   *
   * @param universityId 학교 식별자
   * @return 학과명 오름차순 전체 목록
   */
  List<DepartmentResponse> getDepartments(Long universityId);
}
