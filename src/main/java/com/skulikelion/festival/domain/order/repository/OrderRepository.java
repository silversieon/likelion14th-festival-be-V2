/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse;
import com.skulikelion.festival.domain.order.dto.response.SalesResponse;
import com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse;
import com.skulikelion.festival.domain.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

  @Query(
      """
        SELECT DISTINCT new com.skulikelion.festival.domain.order.dto.response.WaitingOrderResponse(
            o.id,
                o.tableNumber,
                    o.numOfPeople,
                o.customerName,
                    o.customerPhoneNumber,
                        o.createdAt,
                            o.totalOrderPrice
                                ) FROM OrderItem oi
            JOIN oi.order o
                JOIN oi.boothMenu bm
                    WHERE bm.booth.id = :boothId
                        AND oi.order.orderStatus = com.skulikelion.festival.domain.order.entity.enums.OrderStatus.WAITING
                            ORDER BY oi.order.createdAt ASC
    """)
  List<WaitingOrderResponse> findWaitingOrdersByBoothId(@Param("boothId") Long boothId);

  @Query(
      """
        SELECT DISTINCT new com.skulikelion.festival.domain.order.dto.response.CookingOrderResponse(
          o.id,
             o.tableNumber,
                    o.numOfPeople,
            o.customerName,
              o.customerPhoneNumber,
                o.createdAt,
                  o.totalOrderPrice,
                    o.modifiedAt
          ) FROM OrderItem oi
        JOIN oi.order o
          JOIN oi.boothMenu bm
            WHERE bm.booth.id = :boothId
              AND oi.order.orderStatus = com.skulikelion.festival.domain.order.entity.enums.OrderStatus.COOKING
                ORDER BY oi.order.modifiedAt ASC
  """)
  List<CookingOrderResponse> findCookingOrdersByBoothId(@Param("boothId") Long boothId);

  @Query(
      """
    SELECT DISTINCT new com.skulikelion.festival.domain.order.dto.response.CompletedOrderResponse(
        o.id,
        o.tableNumber,
        o.numOfPeople,
        o.customerName,
        o.customerPhoneNumber,
        o.totalOrderPrice,
        o.createdAt,
        o.completedAt
    ) FROM OrderItem oi
    JOIN oi.order o
    JOIN oi.boothMenu bm
    WHERE bm.booth.id = :boothId
    AND oi.order.orderStatus = com.skulikelion.festival.domain.order.entity.enums.OrderStatus.COMPLETED
    AND (:orderDate IS NULL OR FUNCTION('DATE', o.createdAt) = :orderDate)
    AND (:keyword IS NULL OR o.customerName LIKE CONCAT('%', :keyword, '%')
        OR o.customerPhoneNumber LIKE CONCAT('%', :keyword, '%'))
    ORDER BY o.completedAt DESC
""")
  List<CompletedOrderResponse> findCompletedOrdersByBoothIdAndDateAndKeyword(
      @Param("boothId") Long boothId,
      @Param("orderDate") LocalDate orderDate,
      @Param("keyword") String keyword);

  @Query(
      """
    SELECT DISTINCT new com.skulikelion.festival.domain.order.dto.response.CanceledOrderResponse(
        o.id,
        o.tableNumber,
        o.numOfPeople,
        o.customerName,
        o.customerPhoneNumber,
        o.totalOrderPrice,
        o.createdAt,
        o.canceledAt,
        o.orderCancelReason
    ) FROM OrderItem oi
    JOIN oi.order o
    JOIN oi.boothMenu bm
    WHERE bm.booth.id = :boothId
    AND oi.order.orderStatus = com.skulikelion.festival.domain.order.entity.enums.OrderStatus.CANCELED
    AND (:orderDate IS NULL OR FUNCTION('DATE', o.createdAt) = :orderDate)
    AND (:keyword IS NULL OR o.customerName LIKE CONCAT('%', :keyword, '%')
        OR o.customerPhoneNumber LIKE CONCAT('%', :keyword, '%'))
    ORDER BY o.canceledAt DESC
""")
  List<CanceledOrderResponse> findCanceledOrdersByBoothIdAndDateAndKeyword(
      @Param("boothId") Long boothId,
      @Param("orderDate") LocalDate orderDate,
      @Param("keyword") String keyword);

  @Query(
      """
    SELECT new com.skulikelion.festival.domain.order.dto.response.SalesResponse(
        COALESCE(SUM(o.totalOrderPrice), 0)
    )
    FROM Order o
    WHERE o.orderStatus = com.skulikelion.festival.domain.order.entity.enums.OrderStatus.COMPLETED
    AND EXISTS (
        SELECT 1
        FROM OrderItem oi
        JOIN oi.boothMenu bm
        WHERE oi.order = o
        AND bm.booth.id = :boothId
    )
""")
  SalesResponse findCompletedOrderTotalAmountByBoothId(@Param("boothId") Long boothId);

  @Query(
      """
    SELECT new com.skulikelion.festival.domain.order.dto.response.SalesResponse(
        COALESCE(SUM(o.totalOrderPrice), 0)
    )
    FROM Order o
    WHERE o.orderStatus = com.skulikelion.festival.domain.order.entity.enums.OrderStatus.COMPLETED
    AND o.completedAt >= :startDate
    AND o.completedAt < :endDate
    AND EXISTS (
        SELECT 1
        FROM OrderItem oi
        JOIN oi.boothMenu bm
        WHERE oi.order = o
        AND bm.booth.id = :boothId
    )
""")
  SalesResponse findCompletedOrderTotalAmountByBoothIdAndDate(
      @Param("boothId") Long boothId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);
}
