/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_item_units")
public class OrderItemUnit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Builder.Default
  @Column(nullable = false)
  private boolean isServed = false;

  @ManyToOne(fetch = FetchType.LAZY)
  private OrderItem orderItem;

  public void updateServedStatus(boolean served) {
    isServed = served;
  }
}
