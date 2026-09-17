/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.university.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skulikelion.festival.domain.university.dto.response.DepartmentResponse;
import com.skulikelion.festival.domain.university.dto.response.UniversitySearchResponse;
import com.skulikelion.festival.domain.university.service.UniversityService;
import com.skulikelion.festival.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 대학·학과 조회 Controller 입니다.
 *
 * <p>로그인·회원가입 화면에서 학교를 검색하고 학과를 고르는 흐름에 쓰인다. 두 API 모두 인증 없이 호출된다. (LLD-0002)
 *
 * @see UniversityService
 * @since 2026.09.16
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/universities")
@Tag(name = "University", description = "대학·학과 조회 기능을 제공하는 API")
public class UniversityController {

  private final UniversityService universityService;

  @Operation(
      summary = "[ 사용자 | 토큰 X | 학교명 검색 ]",
      description =
          """
          **Parameters**  \n
          name: 검색할 학교명 (앞부분부터 일치) \n
          \n
          **Returns**  \n
          universityId: 학교 식별자 \n
          universityName: 학교명 \n
          """)
  @GetMapping
  public ResponseEntity<BaseResponse<List<UniversitySearchResponse>>> searchUniversities(
      @Parameter(description = "검색할 학교명 (앞부분부터 일치)", example = "서경") @RequestParam(required = false)
          String name) {
    List<UniversitySearchResponse> responses = universityService.searchUniversities(name);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "학교 검색에 성공했습니다.", responses));
  }

  @Operation(
      summary = "[ 사용자 | 토큰 X | 학교별 학과 목록 조회 ]",
      description =
          """
          **Parameters**  \n
          universityId: 학교 식별자 \n
          \n
          **Returns**  \n
          departmentId: 학과 식별자 \n
          departmentName: 학과명 \n
          """)
  @GetMapping("/{universityId}/departments")
  public ResponseEntity<BaseResponse<List<DepartmentResponse>>> getDepartments(
      @Parameter(description = "학교 식별자", example = "1") @PathVariable Long universityId) {
    List<DepartmentResponse> responses = universityService.getDepartments(universityId);
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "학과 목록 조회에 성공했습니다.", responses));
  }
}
