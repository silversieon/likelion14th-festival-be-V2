/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skulikelion.festival.domain.order.entity.OrderIdempotency;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderIdempotencyRepository extends JpaRepository<OrderIdempotency, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("delete from OrderIdempotency oi where oi.createdAt < :threshold")
    int deleteByCreatedAtBefore(@Param("threshold") LocalDateTime threshold);
}
