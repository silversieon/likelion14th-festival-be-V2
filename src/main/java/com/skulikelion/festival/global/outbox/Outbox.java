/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.outbox;

import java.time.LocalDateTime;

import jakarta.persistence.*;

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

  private LocalDateTime createdAt;

  private LocalDateTime publishedAt;
}
