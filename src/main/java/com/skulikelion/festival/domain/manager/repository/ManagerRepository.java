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

public interface ManagerRepository extends JpaRepository<Manager, Long> {
  Optional<Manager> findByUsername(String username);

  @Query(
      """
  SELECT new com.skulikelion.festival.domain.manager.dto.response.ManagerResponse(
    m.id,
      m.username,
        m.role,
          b.name
    )
      FROM Manager m
        LEFT JOIN m.booth b
          WHERE m.role = :role
  """)
  List<ManagerResponse> findManagersWithBoothByRole(@Param("role") Role role);

  @Query(
      """
  SELECT new com.skulikelion.festival.domain.manager.dto.response.ManagerResponse(
    m.id,
      m.username,
        m.role,
          b.name
    )
      FROM Manager m
        LEFT JOIN m.booth b
          WHERE m.id = :managerId
  """)
  Optional<ManagerResponse> findManagerWithBoothById(@Param("managerId") Long managerId);
}
