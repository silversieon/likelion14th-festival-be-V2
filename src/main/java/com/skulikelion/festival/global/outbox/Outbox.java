/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.outbox;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Outbox {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String aggregateId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AggregateType aggregateType;

  private String eventType;

  @Column(columnDefinition = "json")
  private String payload;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime createdAt;

  private LocalDateTime publishedAt;

  public Outbox(String aggregateId, AggregateType aggregateType, String eventType, String payload) {
    this.aggregateId = aggregateId;
    this.aggregateType = aggregateType;
    this.eventType = eventType;
    this.payload = payload;
  }

  public static Outbox of(
      String aggregateId, AggregateType aggregateType, String eventType, String payload) {
    return new Outbox(aggregateId, aggregateType, eventType, payload);
  }

  public void markAsPublished() {
    publishedAt = LocalDateTime.now();
  }
}
