/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.university.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.university.dto.response.DepartmentResponse;
import com.skulikelion.festival.domain.university.dto.response.UniversitySearchResponse;
import com.skulikelion.festival.domain.university.exception.UniversityErrorCode;
import com.skulikelion.festival.domain.university.repository.DepartmentRepository;
import com.skulikelion.festival.domain.university.repository.UniversityRepository;
import com.skulikelion.festival.global.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UniversityServiceImpl implements UniversityService {

  /** JPQL {@code ESCAPE '!'}와 짝을 이루는 LIKE 이스케이프 문자. */
  private static final String LIKE_ESCAPE = "!";

  private final UniversityRepository universityRepository;
  private final DepartmentRepository departmentRepository;

  @Override
  @Transactional(readOnly = true)
  public List<UniversitySearchResponse> searchUniversities(String name) {
    // 필수 @RequestParam 누락은 GlobalExceptionHandler가 500으로 처리하므로, 서비스에서 400으로 바꾼다 (LLD-0002 2.3 R2).
    if (name == null || name.isBlank()) {
      log.info("[UniversityService] 학교명 검색 실패 - 검색어 없음");
      throw new CustomException(UniversityErrorCode.UNIVERSITY_NAME_REQUIRED);
    }

    List<UniversitySearchResponse> responses =
        universityRepository.searchByNamePrefix(escapeLikeWildcards(name.strip()));
    log.debug("[UniversityService] 학교명 검색 - 검색어: {}, 결과 수: {}", name.strip(), responses.size());
    return responses;
  }

  @Override
  @Transactional(readOnly = true)
  public List<DepartmentResponse> getDepartments(Long universityId) {
    if (!universityRepository.existsById(universityId)) {
      log.warn("[UniversityService] 학과 목록 조회 실패 - 존재하지 않는 대학 식별자: {}", universityId);
      throw new CustomException(UniversityErrorCode.UNIVERSITY_NOT_FOUND);
    }
    return departmentRepository.findAllByUniversityId(universityId);
  }

  /**
   * [ LIKE 와일드카드 이스케이프 메서드 ]
   *
   * <p>사용자가 입력한 {@code %}, {@code _}가 와일드카드로 해석되지 않도록 앞에 이스케이프 문자를 붙인다. 이스케이프 문자 자신을 먼저 처리해야 이중 치환이
   * 생기지 않는다.
   */
  private String escapeLikeWildcards(String value) {
    return value
        .replace(LIKE_ESCAPE, LIKE_ESCAPE + LIKE_ESCAPE)
        .replace("%", LIKE_ESCAPE + "%")
        .replace("_", LIKE_ESCAPE + "_");
  }
}
