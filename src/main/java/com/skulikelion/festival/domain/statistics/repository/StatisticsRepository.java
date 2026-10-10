/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.statistics.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.skulikelion.festival.domain.order.entity.Order;
import com.skulikelion.festival.domain.statistics.dto.response.SalesSummaryResponse;
import com.skulikelion.festival.domain.statistics.repository.projection.BoothSalesRow;
import com.skulikelion.festival.domain.statistics.repository.projection.DailySalesRow;
import com.skulikelion.festival.domain.statistics.repository.projection.HourlySalesRow;
import com.skulikelion.festival.domain.statistics.repository.projection.MenuSalesRow;

/**
 * 운영진 통계 조회 전용 Repository 입니다.
 *
 * <p>저장할 것이 없어 {@code JpaRepository}가 아니라 최상위 {@link Repository}를 쓴다. 집계 대상은 항상 {@code COMPLETED}
 * 주문이며 기준 시각은 {@code completed_at}이다. 순위는 MySQL 8 윈도 함수({@code ROW_NUMBER})로 매긴다. (LLD-0005 8.1)
 *
 * <p>검색어 인자는 서비스에서 LIKE 이스케이프(문자 {@code !})를 마친 값이다.
 *
 * @since 2026.10.10
 */
public interface StatisticsRepository extends Repository<Order, Long> {

  @Query(
      """
      SELECT new com.skulikelion.festival.domain.statistics.dto.response.SalesSummaryResponse(
        COUNT(o),
        COALESCE(SUM(o.totalOrderPrice), 0L)
      )
      FROM Order o
      WHERE o.orderStatus = com.skulikelion.festival.domain.order.entity.enums.OrderStatus.COMPLETED
      """)
  SalesSummaryResponse findSalesSummary();

  @Query(
      value =
          """
          SELECT DATE(o.completed_at) AS salesDate,
                 COALESCE(SUM(o.total_order_price), 0) AS totalSales,
                 COUNT(*) AS orderCount
          FROM orders o
          WHERE o.order_status = 'COMPLETED'
            AND o.completed_at >= :startAt
            AND o.completed_at < :endAt
          GROUP BY DATE(o.completed_at)
          ORDER BY salesDate
          """,
      nativeQuery = true)
  List<DailySalesRow> findDailySales(
      @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

  /** {@code startAt}·{@code endAt}이 모두 null이면 전체 기간을 집계한다. */
  @Query(
      value =
          """
          SELECT HOUR(o.completed_at) AS salesHour,
                 COALESCE(SUM(o.total_order_price), 0) AS totalSales,
                 COUNT(*) AS orderCount
          FROM orders o
          WHERE o.order_status = 'COMPLETED'
            AND HOUR(o.completed_at) >= 11
            AND (:startAt IS NULL OR o.completed_at >= :startAt)
            AND (:endAt IS NULL OR o.completed_at < :endAt)
          GROUP BY HOUR(o.completed_at)
          ORDER BY salesHour
          """,
      nativeQuery = true)
  List<HourlySalesRow> findHourlySales(
      @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

  /** 완료 주문이 있는 부스만 매출 순위대로 {@code limit}개. */
  @Query(
      value =
          RANKED_BOOTH_SALES + "WHERE ranked.orderCount > 0 ORDER BY ranked.salesRank LIMIT :limit",
      nativeQuery = true)
  List<BoothSalesRow> findTopBoothSales(@Param("limit") int limit);

  /** 순위를 전체 부스 기준으로 먼저 매긴 뒤 검색 조건으로 거른다. {@code boothName}이 null이면 학교 조건만 건다. */
  @Query(
      value =
          RANKED_BOOTH_SALES
              + """
              WHERE ranked.universityName LIKE CONCAT('%', :universityName, '%') ESCAPE '!'
                AND (:boothName IS NULL OR ranked.departmentName LIKE CONCAT('%', :boothName, '%') ESCAPE '!')
              ORDER BY ranked.salesRank
              """,
      nativeQuery = true)
  List<BoothSalesRow> searchBoothSales(
      @Param("universityName") String universityName, @Param("boothName") String boothName);

  /** 순위는 부스 안에서 매긴다(PARTITION BY 부스). 부스 단위로 거르므로 WHERE가 순위를 바꾸지 않는다. */
  @Query(
      value =
          """
          SELECT ROW_NUMBER() OVER (
                   PARTITION BY b.id
                   ORDER BY COALESCE(s.quantity, 0) DESC, COALESCE(s.sales, 0) DESC, bm.id ASC
                 ) AS menuRank,
                 b.id AS boothId,
                 bm.id AS boothMenuId,
                 u.name AS universityName,
                 d.name AS departmentName,
                 bm.name_ko AS menuName,
                 COALESCE(s.quantity, 0) AS quantity,
                 COALESCE(s.sales, 0) AS sales
          FROM booth_menu bm
          JOIN booth b ON b.id = bm.booth_id
          JOIN departments d ON d.id = b.department_id
          JOIN universities u ON u.id = d.university_id
          LEFT JOIN ("""
              + MENU_SALES
              + """
          ) s ON s.booth_menu_id = bm.id
          WHERE u.name LIKE CONCAT('%', :universityName, '%') ESCAPE '!'
            AND (:boothName IS NULL OR d.name LIKE CONCAT('%', :boothName, '%') ESCAPE '!')
          ORDER BY universityName, departmentName, boothId, menuRank
          """,
      nativeQuery = true)
  List<MenuSalesRow> findBoothMenuRanking(
      @Param("universityName") String universityName, @Param("boothName") String boothName);

  @Query(
      value =
          """
          SELECT ROW_NUMBER() OVER (ORDER BY s.quantity DESC, s.sales DESC, bm.id ASC) AS menuRank,
          """
              + TOP_MENU_COLUMNS_AND_JOINS
              + "ORDER BY menuRank LIMIT :limit",
      nativeQuery = true)
  List<MenuSalesRow> findTopMenusByQuantity(@Param("limit") int limit);

  @Query(
      value =
          """
          SELECT ROW_NUMBER() OVER (ORDER BY s.sales DESC, s.quantity DESC, bm.id ASC) AS menuRank,
          """
              + TOP_MENU_COLUMNS_AND_JOINS
              + "ORDER BY menuRank LIMIT :limit",
      nativeQuery = true)
  List<MenuSalesRow> findTopMenusBySales(@Param("limit") int limit);

  /** 메뉴별 완료 주문 판매량·수익. 팔린 메뉴만 나온다. */
  String MENU_SALES =
      """
      SELECT oi.booth_menu_id,
             SUM(oi.quantity) AS quantity,
             SUM(oi.total_order_item_price) AS sales
      FROM order_items oi
      JOIN orders o ON o.id = oi.order_id
      WHERE o.order_status = 'COMPLETED'
      GROUP BY oi.booth_menu_id
      """;

  /** 전체 부스(주문 없는 부스 포함)에 매출 순위를 매긴 파생 테이블 {@code ranked}. 뒤에 WHERE/ORDER BY를 붙여 쓴다. */
  String RANKED_BOOTH_SALES =
      """
      SELECT ranked.* FROM (
        SELECT ROW_NUMBER() OVER (ORDER BY COALESCE(s.total_sales, 0) DESC, b.id ASC) AS salesRank,
               b.id AS boothId,
               u.name AS universityName,
               d.name AS departmentName,
               COALESCE(s.total_sales, 0) AS totalSales,
               COALESCE(s.order_count, 0) AS orderCount,
               COALESCE(s.total_quantity, 0) AS totalQuantity
        FROM booth b
        JOIN departments d ON d.id = b.department_id
        JOIN universities u ON u.id = d.university_id
        LEFT JOIN (
          SELECT bm.booth_id,
                 SUM(oi.total_order_item_price) AS total_sales,
                 COUNT(DISTINCT oi.order_id) AS order_count,
                 SUM(oi.quantity) AS total_quantity
          FROM order_items oi
          JOIN orders o ON o.id = oi.order_id
          JOIN booth_menu bm ON bm.id = oi.booth_menu_id
          WHERE o.order_status = 'COMPLETED'
          GROUP BY bm.booth_id
        ) s ON s.booth_id = b.id
      ) ranked
      """;

  /** 메뉴 TOP 쿼리의 SELECT 나머지 컬럼과 FROM 절. 앞에 순위 컬럼, 뒤에 ORDER BY/LIMIT을 붙여 쓴다. */
  String TOP_MENU_COLUMNS_AND_JOINS =
      """
             b.id AS boothId,
             bm.id AS boothMenuId,
             u.name AS universityName,
             d.name AS departmentName,
             bm.name_ko AS menuName,
             s.quantity AS quantity,
             s.sales AS sales
      FROM ("""
          + MENU_SALES
          + """
      ) s
      JOIN booth_menu bm ON bm.id = s.booth_menu_id
      JOIN booth b ON b.id = bm.booth_id
      JOIN departments d ON d.id = b.department_id
      JOIN universities u ON u.id = d.university_id
      """;
}
