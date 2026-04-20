/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skulikelion.festival.domain.booth.entity.BoothMenu;

public interface BoothMenuRepository extends JpaRepository<BoothMenu, Long> {

  List<BoothMenu> findByBoothId(Long id);

  Optional<BoothMenu> findByBoothIdAndMenuKo(Long id, String menuKo);

  boolean existsByBoothIdAndMenuKo(Long id, String menuKo);
}
