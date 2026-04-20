/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skulikelion.festival.domain.booth.entity.BoothDetailEn;

public interface BoothDetailEnRepository extends JpaRepository<BoothDetailEn, Long> {

  List<BoothDetailEn> findByBoothFacultyEnContaining(String faculty);

  Optional<BoothDetailEn> findByBoothId(Long id);
}
