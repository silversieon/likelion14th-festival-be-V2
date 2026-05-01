/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skulikelion.festival.domain.booth.dto.response.BoothListResponse;
import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.enums.BoothLocation;
import com.skulikelion.festival.global.enums.Department;
import com.skulikelion.festival.global.enums.Language;

public interface BoothRepository extends JpaRepository<Booth, Long> {

  Optional<Booth> findByDepartment(Department department);

  boolean existsByDepartment(Department department);

  @Query(
      """
      SELECT new com.skulikelion.festival.domain.booth.dto.response.BoothListResponse(
        b.id,
        b.thumbnailUrl,
        b.location,
        b.locationDetail,
        bt.departmentName
      )
      FROM Booth b
      JOIN BoothTranslation bt ON bt.booth = b
      WHERE bt.language = :language
      ORDER BY bt.departmentName ASC
      """)
  List<BoothListResponse> findBoothsByLanguage(@Param("language") Language language);

  @Query(
      """
      SELECT new com.skulikelion.festival.domain.booth.dto.response.BoothListResponse(
        b.id,
        b.thumbnailUrl,
        b.location,
        b.locationDetail,
        bt.departmentName
      )
      FROM Booth b
      JOIN BoothTranslation bt ON bt.booth = b
      WHERE b.location = :location
        AND bt.language = :language
      ORDER BY bt.departmentName ASC
      """)
  List<BoothListResponse> findBoothsByLocationAndLanguage(
      @Param("location") BoothLocation location, @Param("language") Language language);

  @Query(
      """
      SELECT new com.skulikelion.festival.domain.booth.dto.response.BoothListResponse(
        b.id,
        b.thumbnailUrl,
        b.location,
        b.locationDetail,
        bt.departmentName
      )
      FROM Booth b
      JOIN BoothTranslation bt ON bt.booth = b
      WHERE bt.language = :language
        AND LOWER(bt.departmentName) LIKE LOWER(CONCAT('%', :keyword, '%'))
      ORDER BY bt.departmentName ASC
      """)
  List<BoothListResponse> searchBoothsByDepartmentName(
      @Param("keyword") String keyword, @Param("language") Language language);
}
