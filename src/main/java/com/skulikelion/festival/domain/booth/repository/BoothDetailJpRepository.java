/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skulikelion.festival.domain.booth.entity.BoothDetailJp;

public interface BoothDetailJpRepository extends JpaRepository<BoothDetailJp, Long> {

  List<BoothDetailJp> findByBoothFacultyJpContaining(String faculty);

  Optional<BoothDetailJp> findByBoothId(Long id);
}
