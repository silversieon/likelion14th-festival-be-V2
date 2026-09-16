/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.university.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skulikelion.festival.domain.university.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

  Optional<Department> findByUniversityIdAndName(Long universityId, String name);

  boolean existsByUniversityIdAndName(Long universityId, String name);
}
