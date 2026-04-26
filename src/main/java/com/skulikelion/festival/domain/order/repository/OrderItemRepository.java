/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skulikelion.festival.domain.order.dto.response.CanceledOrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderItemResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderItemResponse;
import com.skulikelion.festival.domain.order.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

  @Query(
      """
          SELECT new com.skulikelion.festival.domain.order.dto.response.WaitingOrderItemResponse(
              oi.id,
                  oi.order.id,
                  bm.menuKo,
                      oi.quantity,
                          oi.totalOrderItemPrice
              ) FROM OrderItem oi
                  JOIN oi.boothMenu bm
                      WHERE oi.order.id IN :orderIds
          """)
  List<WaitingOrderItemResponse> findWaitingOrderItemsByOrderIds(
      @Param("orderIds") List<Long> orderIds);

  @Query(
      """
          SELECT new com.skulikelion.festival.domain.order.dto.response.CookingOrderItemResponse(
              oi.id,
                  oi.order.id,
                  bm.menuKo,
                      oi.quantity,
                          oi.totalOrderItemPrice
              ) FROM OrderItem oi
                  JOIN oi.boothMenu bm
                      WHERE oi.order.id IN :orderIds
          """)
  List<CookingOrderItemResponse> findCookingOrderItemsByOrderIds(
      @Param("orderIds") List<Long> orderIds);

  @Query(
      """
    SELECT new com.skulikelion.festival.domain.order.dto.response.CompletedOrderItemResponse(
        oi.id,
        oi.order.id,
        bm.menuKo,
        oi.quantity,
        oi.totalOrderItemPrice
    ) FROM OrderItem oi
    JOIN oi.boothMenu bm
    WHERE oi.order.id IN :orderIds
""")
  List<CompletedOrderItemResponse> findCompletedOrderItemsByOrderIds(
      @Param("orderIds") List<Long> orderIds);

  @Query(
      """
    SELECT new com.skulikelion.festival.domain.order.dto.response.CanceledOrderItemResponse(
        oi.id,
        oi.order.id,
        bm.menuKo,
        oi.quantity,
        oi.totalOrderItemPrice
    ) FROM OrderItem oi
    JOIN oi.boothMenu bm
    WHERE oi.order.id IN :orderIds
""")
  List<CanceledOrderItemResponse> findCanceledOrderItemsByOrderIds(
      @Param("orderIds") List<Long> orderIds);
}
