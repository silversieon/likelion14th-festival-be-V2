/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency.strategy.db.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skulikelion.festival.global.util.idempotency.strategy.db.entity.Idempotency;

public interface IdempotencyRepository extends JpaRepository<Idempotency, UUID> {

  @Modifying(clearAutomatically = true)
  @Query("delete from Idempotency oi where oi.createdAt < :threshold")
  int deleteByCreatedAtBefore(@Param("threshold") LocalDateTime threshold);
}
