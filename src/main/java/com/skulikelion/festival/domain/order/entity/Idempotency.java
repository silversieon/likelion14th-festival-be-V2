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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Idempotency extends BaseTimeEntity implements Persistable<UUID> {

  @Id
  @JdbcTypeCode(SqlTypes.BINARY)
  @Column(name = "idempotency_key", length = 16)
  private UUID idempotency_key;

  @Column(name = "response_body", columnDefinition = "json")
  private String responseBody;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private IdempotencyStatus status;

  public static Idempotency processing(UUID key) {
    Idempotency e = new Idempotency();
    e.idempotency_key = key;
    e.status = IdempotencyStatus.PROCESSING;
    return e;
  }

  public void markDone(String responseBody) {
    this.status = IdempotencyStatus.DONE;
    this.responseBody = responseBody;
  }

  @Override
  public @Nullable UUID getId() {
    return idempotency_key;
  }

  @Override
  public boolean isNew() {
    return this.status == IdempotencyStatus.PROCESSING && this.responseBody == null;
  }
}
