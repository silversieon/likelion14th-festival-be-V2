/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skulikelion.festival.domain.order.dto.response.CookingOrderItemUnitResponse;
import com.skulikelion.festival.domain.order.entity.OrderItemUnit;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;

public interface OrderItemUnitRepository extends JpaRepository<OrderItemUnit, Long> {

  @Query(
      """
    SELECT o.orderStatus FROM OrderItemUnit oiu
    JOIN oiu.orderItem oi
    JOIN oi.order o
    WHERE oiu.id = :orderItemUnitId
""")
  Optional<OrderStatus> findOrderStatusByOrderItemUnitId(
      @Param("orderItemUnitId") Long orderItemUnitId);

  @Query(
      """
            SELECT new com.skulikelion.festival.domain.order.dto.response.CookingOrderItemUnitResponse(
                oiu.id,
                             oiu.orderItem.id,
                                        oiu.isServed
                ) FROM OrderItemUnit oiu
                        WHERE oiu.orderItem.id IN :orderItemIds
            """)
  List<CookingOrderItemUnitResponse> findCookingOrderItemUnitsByOrderItemIds(
      @Param("orderItemIds") List<Long> orderItemIds);
}
