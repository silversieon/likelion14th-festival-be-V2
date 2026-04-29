/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.repository;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skulikelion.festival.domain.lostitem.entity.LostItem;

@Repository
public interface LostItemRepository extends JpaRepository<LostItem, Long> {

  Page<LostItem> findAllByOrderByCreatedAtDesc(Pageable pageable);

  Page<LostItem> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String name, Pageable pageable);

  Page<LostItem> findByFoundDateOrderByCreatedAtDesc(LocalDate foundDate, Pageable pageable);

  Page<LostItem> findByNameContainingIgnoreCaseAndFoundDateOrderByCreatedAtDesc(
      String name, LocalDate foundDate, Pageable pageable);
}
