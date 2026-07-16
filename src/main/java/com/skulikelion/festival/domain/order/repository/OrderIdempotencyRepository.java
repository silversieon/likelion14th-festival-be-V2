/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skulikelion.festival.domain.order.entity.OrderIdempotency;

public interface OrderIdempotencyRepository extends JpaRepository<OrderIdempotency, UUID> {}
