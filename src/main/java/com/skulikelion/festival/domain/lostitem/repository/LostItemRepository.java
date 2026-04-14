package com.skulikelion.festival.domain.lostitem.repository;

import com.skulikelion.festival.domain.lostitem.entity.LostItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LostItemRepository extends JpaRepository<LostItem, Long> {

  Page<LostItem> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

  Page<LostItem> findByIsDeletedFalseOrderByCreatedAtAsc(Pageable pageable);

  Page<LostItem> findByIsDeletedFalseAndNameContainingIgnoreCaseOrderByCreatedAtDesc(
      String name, Pageable pageable);

  Page<LostItem> findByIsDeletedFalseAndNameContainingIgnoreCaseOrderByCreatedAtAsc(
      String name, Pageable pageable);

  List<LostItem> findByIsDeletedTrueOrderByCreatedAtDesc();
}
