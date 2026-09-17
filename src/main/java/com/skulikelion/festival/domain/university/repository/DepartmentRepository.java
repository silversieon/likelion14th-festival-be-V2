/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.university.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skulikelion.festival.domain.university.dto.response.DepartmentResponse;
import com.skulikelion.festival.domain.university.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

  Optional<Department> findByUniversityIdAndName(Long universityId, String name);

  boolean existsByUniversityIdAndName(Long universityId, String name);

  /**
   * [ 학교별 학과 전체 조회 ]
   *
   * <p>{@code d.university.id}는 FK 컬럼만 읽어 조인이 없다. 조건·정렬이 {@code uq_departments_university_name
   * (university_id, name)} 인덱스 순서와 같아 filesort 없이 처리된다. (LLD-0002 8장)
   */
  @Query(
      """
      SELECT new com.skulikelion.festival.domain.university.dto.response.DepartmentResponse(
        d.id,
        d.name
      )
      FROM Department d
      WHERE d.university.id = :universityId
      ORDER BY d.name ASC
      """)
  List<DepartmentResponse> findAllByUniversityId(@Param("universityId") Long universityId);
}
