/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skulikelion.festival.domain.booth.entity.BoothDetailCh;

public interface BoothDetailChRepository extends JpaRepository<BoothDetailCh, Long> {

  List<BoothDetailCh> findByBoothFacultyChContaining(String faculty);

  Optional<BoothDetailCh> findByBoothId(Long id);
}
