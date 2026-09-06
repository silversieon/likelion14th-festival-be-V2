/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

  @Query(
      value =
          """
        SELECT * FROM outbox
        WHERE published_at IS NULL
        ORDER BY id ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """,
      nativeQuery = true)
  List<Outbox> findPendingEvents(@Param("limit") Integer limit);
}
