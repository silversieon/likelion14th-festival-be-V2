/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.statistics.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.statistics.dto.response.BoothMenuSalesResponse;
import com.skulikelion.festival.domain.statistics.dto.response.BoothSalesResponse;
import com.skulikelion.festival.domain.statistics.dto.response.DailySalesResponse;
import com.skulikelion.festival.domain.statistics.dto.response.HourlySalesResponse;
import com.skulikelion.festival.domain.statistics.dto.response.MenuSalesResponse;
import com.skulikelion.festival.domain.statistics.dto.response.SalesSummaryResponse;
import com.skulikelion.festival.domain.statistics.enums.MenuSortType;
import com.skulikelion.festival.domain.statistics.exception.StatisticsErrorCode;
import com.skulikelion.festival.domain.statistics.repository.StatisticsRepository;
import com.skulikelion.festival.domain.statistics.repository.projection.BoothSalesRow;
import com.skulikelion.festival.domain.statistics.repository.projection.DailySalesRow;
import com.skulikelion.festival.domain.statistics.repository.projection.HourlySalesRow;
import com.skulikelion.festival.domain.statistics.repository.projection.MenuSalesRow;
import com.skulikelion.festival.global.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

  /** 시간대별 추이의 첫 구간 (11:00 ~ 12:00). */
  private static final int FIRST_HOUR = 11;

  /** 시간대별 추이의 마지막 구간 (23:00 ~ 24:00). */
  private static final int LAST_HOUR = 23;

  private static final int TOP_LIMIT = 10;

  /** JPQL·SQL {@code ESCAPE '!'}와 짝을 이루는 LIKE 이스케이프 문자. */
  private static final String LIKE_ESCAPE = "!";

  private final StatisticsRepository statisticsRepository;

  @Override
  @Transactional(readOnly = true)
  public SalesSummaryResponse getSalesSummary() {
    return statisticsRepository.findSalesSummary();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DailySalesResponse> getDailySales(LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null) {
      log.info("[StatisticsService] 일별 매출 조회 실패 - 기간 누락: {} ~ {}", startDate, endDate);
      throw new CustomException(StatisticsErrorCode.STATISTICS_DATE_RANGE_REQUIRED);
    }
    validateDateOrder(startDate, endDate);

    Map<LocalDate, DailySalesRow> rowsByDate =
        statisticsRepository
            .findDailySales(startOf(startDate), startOf(endDate.plusDays(1)))
            .stream()
            .collect(Collectors.toMap(DailySalesRow::getSalesDate, Function.identity()));

    return startDate
        .datesUntil(endDate.plusDays(1))
        .map(date -> toDailySalesResponse(date, rowsByDate.get(date)))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<HourlySalesResponse> getHourlySales(LocalDate startDate, LocalDate endDate) {
    if ((startDate == null) != (endDate == null)) {
      log.info("[StatisticsService] 시간대별 매출 조회 실패 - 기간 한쪽 누락: {} ~ {}", startDate, endDate);
      throw new CustomException(StatisticsErrorCode.STATISTICS_DATE_RANGE_REQUIRED);
    }

    List<HourlySalesRow> rows;
    if (startDate == null) {
      rows = statisticsRepository.findHourlySales(null, null);
    } else {
      validateDateOrder(startDate, endDate);
      rows = statisticsRepository.findHourlySales(startOf(startDate), startOf(endDate.plusDays(1)));
    }

    Map<Integer, HourlySalesRow> rowsByHour =
        rows.stream().collect(Collectors.toMap(HourlySalesRow::getSalesHour, Function.identity()));

    return IntStream.rangeClosed(FIRST_HOUR, LAST_HOUR)
        .mapToObj(hour -> toHourlySalesResponse(hour, rowsByHour.get(hour)))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<BoothSalesResponse> getTopBoothSales() {
    return statisticsRepository.findTopBoothSales(TOP_LIMIT).stream()
        .map(this::toBoothSalesResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<BoothSalesResponse> searchBoothSales(String universityName, String boothName) {
    return statisticsRepository
        .searchBoothSales(requireUniversityKeyword(universityName), optionalKeyword(boothName))
        .stream()
        .map(this::toBoothSalesResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<BoothMenuSalesResponse> getBoothMenuRanking(String universityName, String boothName) {
    return statisticsRepository
        .findBoothMenuRanking(requireUniversityKeyword(universityName), optionalKeyword(boothName))
        .stream()
        .map(this::toBoothMenuSalesResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<MenuSalesResponse> getTopMenus(MenuSortType sortType) {
    List<MenuSalesRow> rows =
        switch (sortType) {
          case QUANTITY -> statisticsRepository.findTopMenusByQuantity(TOP_LIMIT);
          case SALES -> statisticsRepository.findTopMenusBySales(TOP_LIMIT);
        };
    return rows.stream().map(this::toMenuSalesResponse).toList();
  }

  private void validateDateOrder(LocalDate startDate, LocalDate endDate) {
    if (startDate.isAfter(endDate)) {
      log.info("[StatisticsService] 매출 조회 실패 - 시작일이 종료일보다 늦음: {} ~ {}", startDate, endDate);
      throw new CustomException(StatisticsErrorCode.STATISTICS_INVALID_DATE_RANGE);
    }
  }

  private LocalDateTime startOf(LocalDate date) {
    return date.atStartOfDay();
  }

  /** 학교명은 필수다. 앞뒤 공백을 지우고 LIKE 와일드카드를 이스케이프한다. */
  private String requireUniversityKeyword(String universityName) {
    String keyword = optionalKeyword(universityName);
    if (keyword == null) {
      log.info("[StatisticsService] 부스 통계 검색 실패 - 학교명 없음");
      throw new CustomException(StatisticsErrorCode.STATISTICS_UNIVERSITY_NAME_REQUIRED);
    }
    return keyword;
  }

  /** 없거나 공백뿐이면 null(조건 없음), 아니면 앞뒤 공백을 지우고 LIKE 와일드카드를 이스케이프한다. */
  private String optionalKeyword(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return escapeLikeWildcards(value.strip());
  }

  /**
   * [ LIKE 와일드카드 이스케이프 메서드 ]
   *
   * <p>사용자가 입력한 {@code %}, {@code _}가 와일드카드로 해석되지 않도록 앞에 이스케이프 문자를 붙인다. 이스케이프 문자 자신을 먼저 처리해야 이중 치환이
   * 생기지 않는다.
   */
  private String escapeLikeWildcards(String value) {
    return value
        .replace(LIKE_ESCAPE, LIKE_ESCAPE + LIKE_ESCAPE)
        .replace("%", LIKE_ESCAPE + "%")
        .replace("_", LIKE_ESCAPE + "_");
  }

  private DailySalesResponse toDailySalesResponse(LocalDate date, DailySalesRow row) {
    return DailySalesResponse.builder()
        .date(date)
        .totalSales(row == null ? 0L : row.getTotalSales())
        .orderCount(row == null ? 0L : row.getOrderCount())
        .build();
  }

  private HourlySalesResponse toHourlySalesResponse(int hour, HourlySalesRow row) {
    return HourlySalesResponse.builder()
        .hour(hour)
        .totalSales(row == null ? 0L : row.getTotalSales())
        .orderCount(row == null ? 0L : row.getOrderCount())
        .build();
  }

  private BoothSalesResponse toBoothSalesResponse(BoothSalesRow row) {
    return BoothSalesResponse.builder()
        .rank(row.getSalesRank())
        .boothId(row.getBoothId())
        .universityName(row.getUniversityName())
        .departmentName(row.getDepartmentName())
        .totalSales(row.getTotalSales())
        .averageOrderAmount(averageOrderAmount(row.getTotalSales(), row.getOrderCount()))
        .orderCount(row.getOrderCount())
        .totalQuantity(row.getTotalQuantity())
        .build();
  }

  /** 총 매출 ÷ 주문 건수를 반올림한다. 주문이 없으면 0이다. (LLD-0005 2.3 R8) */
  private long averageOrderAmount(long totalSales, long orderCount) {
    return orderCount == 0 ? 0L : Math.round((double) totalSales / orderCount);
  }

  private BoothMenuSalesResponse toBoothMenuSalesResponse(MenuSalesRow row) {
    return BoothMenuSalesResponse.builder()
        .rank(row.getMenuRank())
        .boothId(row.getBoothId())
        .boothMenuId(row.getBoothMenuId())
        .universityName(row.getUniversityName())
        .departmentName(row.getDepartmentName())
        .menuName(row.getMenuName())
        .quantity(row.getQuantity())
        .sales(row.getSales())
        .build();
  }

  private MenuSalesResponse toMenuSalesResponse(MenuSalesRow row) {
    return MenuSalesResponse.builder()
        .rank(row.getMenuRank())
        .boothId(row.getBoothId())
        .boothMenuId(row.getBoothMenuId())
        .universityName(row.getUniversityName())
        .departmentName(row.getDepartmentName())
        .menuName(row.getMenuName())
        .quantity(row.getQuantity())
        .sales(row.getSales())
        .build();
  }
}
