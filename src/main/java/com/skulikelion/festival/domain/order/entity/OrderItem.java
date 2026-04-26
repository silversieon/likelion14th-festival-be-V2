/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.entity;

import jakarta.persistence.*;

import com.skulikelion.festival.domain.booth.entity.BoothMenu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_items")
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Integer quantity;

  private Integer menuPrice;

  private Integer totalOrderItemPrice;

  @ManyToOne(fetch = FetchType.LAZY)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  private BoothMenu boothMenu;
}
