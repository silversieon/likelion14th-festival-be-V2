/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.university.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skulikelion.festival.domain.university.dto.response.UniversitySearchResponse;
import com.skulikelion.festival.domain.university.entity.University;
import com.skulikelion.festival.domain.university.enums.Region;

public interface UniversityRepository extends JpaRepository<University, Long> {

  boolean existsByNameAndRegion(String name, Region region);

  /**
   * [ 학교명 전방 일치 검색 ]
   *
   * <p>전방 일치라 {@code uq_universities_name_region (name, region)} 인덱스의 범위 스캔으로 처리된다. 중간 일치({@code
   * %keyword%})였다면 풀스캔이다. (LLD-0002 8장)
   *
   * <p>이스케이프 문자를 {@code \}가 아니라 {@code !}로 둔 이유: MySQL은 문자열 리터럴 안의 백슬래시를 이스케이프로 해석해 {@code ESCAPE
   * '\'}가 문법 오류가 될 수 있다.
   *
   * @param escapedPrefix {@code !}, {@code %}, {@code _}가 {@code !}로 이스케이프된 검색어
   */
  @Query(
      """
      SELECT new com.skulikelion.festival.domain.university.dto.response.UniversitySearchResponse(
        u.id,
        u.name
      )
      FROM University u
      WHERE u.name LIKE CONCAT(:prefix, '%') ESCAPE '!'
      ORDER BY u.name ASC
      """)
  List<UniversitySearchResponse> searchByNamePrefix(@Param("prefix") String escapedPrefix);
}
