/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skulikelion.festival.domain.university.entity.University;
import com.skulikelion.festival.domain.university.enums.Region;

public interface UniversityRepository extends JpaRepository<University, Long> {

  boolean existsByNameAndRegion(String name, Region region);
}
