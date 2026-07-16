/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.entity;

import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Persistable;

import com.skulikelion.festival.domain.order.entity.enums.IdempotencyStatus;
import com.skulikelion.festival.global.common.BaseTimeEntity;

import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderIdempotency extends BaseTimeEntity implements Persistable<UUID> {

  @Id
  @JdbcTypeCode(SqlTypes.BINARY)
  @Column(name = "idempotency_key", length = 16)
  private UUID idempotencyKey;

  @Column(name = "response_body", columnDefinition = "json")
  private String responseBody;

  @Enumerated(EnumType.STRING)
  @Column(name = "idempotency_status", nullable = false, length = 20)
  private IdempotencyStatus idempotencyStatus;

  public static OrderIdempotency processing(UUID key) {
    OrderIdempotency e = new OrderIdempotency();
    e.idempotencyKey = key;
    e.idempotencyStatus = IdempotencyStatus.PROCESSING;
    return e;
  }

  public void markDone(String responseBody) {
    this.idempotencyStatus = IdempotencyStatus.DONE;
    this.responseBody = responseBody;
  }

  @Override
  public @Nullable UUID getId() {
    return idempotencyKey;
  }

  @Override
  public boolean isNew() {
    return this.idempotencyStatus == IdempotencyStatus.PROCESSING && this.responseBody == null;
  }
}
