/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skulikelion.festival.domain.booth.entity.BoothMenu;

public interface BoothMenuRepository extends JpaRepository<BoothMenu, Long> {

  List<BoothMenu> findByBoothIdOrderByIdAsc(Long boothId);

  void deleteByBoothId(Long boothId);
}
