/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skulikelion.festival.domain.lostitem.entity.LostItemImage;

@Repository
public interface LostItemImageRepository extends JpaRepository<LostItemImage, Long> {

  List<LostItemImage> findByLostItemIdOrderByIdAsc(Long lostItemId);

  void deleteByLostItemId(Long lostItemId);
}
