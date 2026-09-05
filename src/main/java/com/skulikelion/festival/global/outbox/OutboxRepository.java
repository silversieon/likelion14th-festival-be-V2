/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

  @Query("""
    SELECT o FROM Outbox o
    WHERE o.publishedAt IS NULL
""")
  List<Outbox> findPendingEvents();
}
