/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.skulikelion.festival.domain.order.entity.enums.OrderCancelReason;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;
import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.global.common.BaseTimeEntity;
import com.skulikelion.festival.global.exception.CustomException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Integer tableNumber;

  private Integer numOfPeople;

  @Column(nullable = false)
  private String customerName;

  @Column(nullable = false)
  private String customerPhoneNumber;

  private Integer totalOrderPrice;

  private LocalDateTime completedAt;

  private LocalDateTime canceledAt;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  private OrderStatus orderStatus = OrderStatus.WAITING;

  @Enumerated(EnumType.STRING)
  private OrderCancelReason orderCancelReason;

  public void changeOrderStatus(OrderStatus newOrderStatus) {
    if (this.orderStatus == newOrderStatus) return;
    if (newOrderStatus == OrderStatus.CANCELED)
      throw new CustomException(OrderErrorCode.ORDER_STATUS_CHANGE_FAILED);
    if (!this.orderStatus.canChangeTo(newOrderStatus)) {
      throw new CustomException(OrderErrorCode.ORDER_STATUS_CHANGE_FAILED);
    }
    this.orderStatus = newOrderStatus;
    if (newOrderStatus == OrderStatus.COMPLETED) this.completedAt = LocalDateTime.now();
  }

  public void cancelOrder(OrderCancelReason orderCancelReason) {
    if (!this.orderStatus.canChangeTo(OrderStatus.CANCELED)) {
      throw new CustomException(OrderErrorCode.ORDER_CANCEL_FAILED);
    }
    this.orderStatus = OrderStatus.CANCELED;
    this.orderCancelReason = orderCancelReason;
    this.canceledAt = LocalDateTime.now();
  }
}
