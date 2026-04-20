/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skulikelion.festival.domain.booth.entity.BoothDetailKo;

public interface BoothDetailKoRepository extends JpaRepository<BoothDetailKo, Long> {

  List<BoothDetailKo> findByBoothFacultyKoContaining(String faculty);

  Optional<BoothDetailKo> findByBoothId(Long id);
}
