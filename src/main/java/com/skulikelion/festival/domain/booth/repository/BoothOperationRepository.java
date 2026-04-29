/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skulikelion.festival.domain.booth.entity.BoothOperation;

public interface BoothOperationRepository extends JpaRepository<BoothOperation, Long> {

  List<BoothOperation> findByBoothIdOrderByOperationDateAsc(Long boothId);

  Optional<BoothOperation> findByBoothIdAndOperationDate(Long boothId, LocalDate operationDate);

  void deleteByBoothId(Long boothId);
}
