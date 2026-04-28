/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skulikelion.festival.domain.booth.entity.BoothDetailImage;

public interface BoothDetailImageRepository extends JpaRepository<BoothDetailImage, Long> {

  List<BoothDetailImage> findByBoothIdOrderByIdAsc(Long boothId);

  void deleteByBoothId(Long boothId);
}
