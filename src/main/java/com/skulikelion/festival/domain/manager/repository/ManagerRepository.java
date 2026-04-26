/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.manager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skulikelion.festival.domain.manager.dto.response.ManagerResponse;
import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.global.enums.Department;

public interface ManagerRepository extends JpaRepository<Manager, Long> {
  Optional<Manager> findByDepartment(Department department);

  @Query(
      """
  SELECT new com.skulikelion.festival.domain.manager.dto.response.ManagerResponse(
    m.id,
      m.department,
        m.role
    ) FROM Manager m
      WHERE m.role = :role
  """)
  List<ManagerResponse> findManagersByRole(@Param("role") Role role);

  @Query(
      """
  SELECT new com.skulikelion.festival.domain.manager.dto.response.ManagerResponse(
    m.id,
      m.department,
        m.role
    ) FROM Manager m
          WHERE m.id = :managerId
  """)
  Optional<ManagerResponse> findManagerById(@Param("managerId") Long managerId);
}
