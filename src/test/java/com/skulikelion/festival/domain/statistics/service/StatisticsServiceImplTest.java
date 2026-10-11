/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

/**
 * {@link StatisticsServiceImpl} 테스트입니다. (LLD-0005 13.1)
 *
 * <p>리포지토리는 Mock이다. 실제 SQL은 {@code StatisticsApiIntegrationTest}가 검증한다.
 *
 * @since 2026.10.10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StatisticsServiceImpl은")
class StatisticsServiceImplTest {

  @Mock private StatisticsRepository statisticsRepository;

  @InjectMocks private StatisticsServiceImpl statisticsService;

  private static final LocalDate OCT_9 = LocalDate.of(2026, 10, 9);
  private static final LocalDate OCT_10 = LocalDate.of(2026, 10, 10);
  private static final LocalDate OCT_11 = LocalDate.of(2026, 10, 11);

  private static StatisticsErrorCode errorCodeOf(Throwable e) {
    return (StatisticsErrorCode) ((CustomException) e).getErrorCode();
  }

  private static DailySalesRow dailyRow(LocalDate date, long totalSales, long orderCount) {
    return new DailySalesRow() {
      public LocalDate getSalesDate() {
        return date;
      }

      public Long getTotalSales() {
        return totalSales;
      }

      public Long getOrderCount() {
        return orderCount;
      }
    };
  }

  private static HourlySalesRow hourlyRow(int hour, long totalSales, long orderCount) {
    return new HourlySalesRow() {
      public Integer getSalesHour() {
        return hour;
      }

      public Long getTotalSales() {
        return totalSales;
      }

      public Long getOrderCount() {
        return orderCount;
      }
    };
  }

  private static BoothSalesRow boothRow(long rank, long totalSales, long orderCount) {
    return new BoothSalesRow() {
      public Long getSalesRank() {
        return rank;
      }

      public Long getBoothId() {
        return 100L + rank;
      }

      public String getUniversityName() {
        return "서경대학교";
      }

      public String getDepartmentName() {
        return "소프트웨어학과";
      }

      public Long getTotalSales() {
        return totalSales;
      }

      public Long getOrderCount() {
        return orderCount;
      }

      public Long getTotalQuantity() {
        return 7L;
      }
    };
  }

  private static MenuSalesRow menuRow(long rank, String menuName) {
    return new MenuSalesRow() {
      public Long getMenuRank() {
        return rank;
      }

      public Long getBoothId() {
        return 1L;
      }

      public Long getBoothMenuId() {
        return 10L + rank;
      }

      public String getUniversityName() {
        return "서경대학교";
      }

      public String getDepartmentName() {
        return "소프트웨어학과";
      }

      public String getMenuName() {
        return menuName;
      }

      public Long getQuantity() {
        return 5L;
      }

      public Long getSales() {
        return 20000L;
      }
    };
  }

  @Nested
  @DisplayName("getSalesSummary는")
  class GetSalesSummary {

    @Test
    @DisplayName("리포지토리가 준 총계를 그대로 반환한다")
    void returnsRepositorySummary() {
      SalesSummaryResponse expected = new SalesSummaryResponse(3L, 45000L);
      given(statisticsRepository.findSalesSummary()).willReturn(expected);

      SalesSummaryResponse result = statisticsService.getSalesSummary();

      assertThat(result).isSameAs(expected);
    }
  }

  @Nested
  @DisplayName("getDailySales는")
  class GetDailySales {

    @Test
    @DisplayName("시작일이나 종료일이 null이면 STATISTICS_DATE_RANGE_REQUIRED를 던진다")
    void throwsRangeRequired_whenDateMissing() {
      assertThatThrownBy(() -> statisticsService.getDailySales(null, OCT_10))
          .isInstanceOf(CustomException.class)
          .extracting(StatisticsServiceImplTest::errorCodeOf)
          .isEqualTo(StatisticsErrorCode.STATISTICS_DATE_RANGE_REQUIRED);
      assertThatThrownBy(() -> statisticsService.getDailySales(OCT_10, null))
          .isInstanceOf(CustomException.class)
          .extracting(StatisticsServiceImplTest::errorCodeOf)
          .isEqualTo(StatisticsErrorCode.STATISTICS_DATE_RANGE_REQUIRED);
      verify(statisticsRepository, never()).findDailySales(any(), any());
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 STATISTICS_INVALID_DATE_RANGE를 던진다")
    void throwsInvalidRange_whenStartAfterEnd() {
      assertThatThrownBy(() -> statisticsService.getDailySales(OCT_11, OCT_10))
          .isInstanceOf(CustomException.class)
          .extracting(StatisticsServiceImplTest::errorCodeOf)
          .isEqualTo(StatisticsErrorCode.STATISTICS_INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("리포지토리에 [시작일 00:00, 종료일+1일 00:00) 경계를 넘긴다")
    void passesHalfOpenBoundaries() {
      given(statisticsRepository.findDailySales(any(), any())).willReturn(List.of());

      statisticsService.getDailySales(OCT_9, OCT_10);

      verify(statisticsRepository)
          .findDailySales(
              LocalDateTime.of(2026, 10, 9, 0, 0), LocalDateTime.of(2026, 10, 11, 0, 0));
    }

    @Test
    @DisplayName("주문이 없는 날짜를 0으로 채워 기간의 모든 날짜를 오름차순으로 반환한다")
    void fillsMissingDatesWithZero() {
      given(statisticsRepository.findDailySales(any(), any()))
          .willReturn(List.of(dailyRow(OCT_10, 30000L, 2L)));

      List<DailySalesResponse> result = statisticsService.getDailySales(OCT_9, OCT_11);

      assertThat(result)
          .extracting(
              DailySalesResponse::getDate,
              DailySalesResponse::getTotalSales,
              DailySalesResponse::getOrderCount)
          .containsExactly(tuple(OCT_9, 0L, 0L), tuple(OCT_10, 30000L, 2L), tuple(OCT_11, 0L, 0L));
    }

    @Test
    @DisplayName("시작일과 종료일이 같으면 하루치 1건을 반환한다")
    void returnsSingleDay_whenSameDate() {
      given(statisticsRepository.findDailySales(any(), any())).willReturn(List.of());

      List<DailySalesResponse> result = statisticsService.getDailySales(OCT_10, OCT_10);

      assertThat(result).extracting(DailySalesResponse::getDate).containsExactly(OCT_10);
    }
  }

  @Nested
  @DisplayName("getHourlySales는")
  class GetHourlySales {

    @Test
    @DisplayName("시작일·종료일이 모두 null이면 리포지토리에 null 경계를 넘긴다")
    void passesNullBoundaries_whenAllPeriod() {
      given(statisticsRepository.findHourlySales(any(), any())).willReturn(List.of());

      statisticsService.getHourlySales(null, null);

      verify(statisticsRepository).findHourlySales(null, null);
    }

    @Test
    @DisplayName("기간을 주면 [시작일 00:00, 종료일+1일 00:00) 경계를 넘긴다")
    void passesHalfOpenBoundaries_whenPeriod() {
      given(statisticsRepository.findHourlySales(any(), any())).willReturn(List.of());

      statisticsService.getHourlySales(OCT_9, OCT_10);

      verify(statisticsRepository)
          .findHourlySales(
              LocalDateTime.of(2026, 10, 9, 0, 0), LocalDateTime.of(2026, 10, 11, 0, 0));
    }

    @Test
    @DisplayName("둘 중 하나만 null이면 STATISTICS_DATE_RANGE_REQUIRED를 던진다")
    void throwsRangeRequired_whenOnlyOneDate() {
      assertThatThrownBy(() -> statisticsService.getHourlySales(OCT_10, null))
          .isInstanceOf(CustomException.class)
          .extracting(StatisticsServiceImplTest::errorCodeOf)
          .isEqualTo(StatisticsErrorCode.STATISTICS_DATE_RANGE_REQUIRED);
      assertThatThrownBy(() -> statisticsService.getHourlySales(null, OCT_10))
          .isInstanceOf(CustomException.class)
          .extracting(StatisticsServiceImplTest::errorCodeOf)
          .isEqualTo(StatisticsErrorCode.STATISTICS_DATE_RANGE_REQUIRED);
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 STATISTICS_INVALID_DATE_RANGE를 던진다")
    void throwsInvalidRange_whenStartAfterEnd() {
      assertThatThrownBy(() -> statisticsService.getHourlySales(OCT_11, OCT_10))
          .isInstanceOf(CustomException.class)
          .extracting(StatisticsServiceImplTest::errorCodeOf)
          .isEqualTo(StatisticsErrorCode.STATISTICS_INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("11시부터 23시까지 13개 구간을 반환하고 주문이 없는 구간은 0으로 채운다")
    void returnsThirteenHoursFilledWithZero() {
      given(statisticsRepository.findHourlySales(any(), any()))
          .willReturn(List.of(hourlyRow(11, 8000L, 1L), hourlyRow(23, 12000L, 2L)));

      List<HourlySalesResponse> result = statisticsService.getHourlySales(null, null);

      assertThat(result).hasSize(13);
      assertThat(result)
          .extracting(HourlySalesResponse::getHour)
          .containsExactly(11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23);
      assertThat(result.get(0))
          .extracting(HourlySalesResponse::getTotalSales, HourlySalesResponse::getOrderCount)
          .containsExactly(8000L, 1L);
      assertThat(result.get(1))
          .extracting(HourlySalesResponse::getTotalSales, HourlySalesResponse::getOrderCount)
          .containsExactly(0L, 0L);
      assertThat(result.get(12))
          .extracting(HourlySalesResponse::getTotalSales, HourlySalesResponse::getOrderCount)
          .containsExactly(12000L, 2L);
    }
  }

  @Nested
  @DisplayName("getTopBoothSales는")
  class GetTopBoothSales {

    @Test
    @DisplayName("리포지토리에 limit 10을 넘기고 결과를 응답으로 옮긴다")
    void requestsTopTenAndMapsRows() {
      given(statisticsRepository.findTopBoothSales(10))
          .willReturn(List.of(boothRow(1, 30000L, 3L)));

      List<BoothSalesResponse> result = statisticsService.getTopBoothSales();

      assertThat(result)
          .extracting(
              BoothSalesResponse::getRank,
              BoothSalesResponse::getBoothId,
              BoothSalesResponse::getUniversityName,
              BoothSalesResponse::getDepartmentName,
              BoothSalesResponse::getTotalSales,
              BoothSalesResponse::getOrderCount,
              BoothSalesResponse::getTotalQuantity)
          .containsExactly(tuple(1L, 101L, "서경대학교", "소프트웨어학과", 30000L, 3L, 7L));
    }

    @Test
    @DisplayName("평균 주문 금액은 총 매출 ÷ 주문 건수를 반올림한 값이다")
    void roundsAverageOrderAmount() {
      given(statisticsRepository.findTopBoothSales(10))
          .willReturn(List.of(boothRow(1, 10000L, 3L), boothRow(2, 10000L, 6L)));

      List<BoothSalesResponse> result = statisticsService.getTopBoothSales();

      // 10000 / 3 = 3333.33 → 3333, 10000 / 6 = 1666.67 → 1667
      assertThat(result)
          .extracting(BoothSalesResponse::getAverageOrderAmount)
          .containsExactly(3333L, 1667L);
    }

    @Test
    @DisplayName("주문 건수가 0이면 평균 주문 금액은 0이다")
    void averageIsZero_whenNoOrders() {
      given(statisticsRepository.findTopBoothSales(10)).willReturn(List.of(boothRow(1, 0L, 0L)));

      List<BoothSalesResponse> result = statisticsService.getTopBoothSales();

      assertThat(result).extracting(BoothSalesResponse::getAverageOrderAmount).containsExactly(0L);
    }
  }

  @Nested
  @DisplayName("searchBoothSales는")
  class SearchBoothSales {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("학교명이 없거나 공백뿐이면 STATISTICS_UNIVERSITY_NAME_REQUIRED를 던진다")
    void throwsUniversityNameRequired_whenBlank(String universityName) {
      assertThatThrownBy(() -> statisticsService.searchBoothSales(universityName, "소프트웨어"))
          .isInstanceOf(CustomException.class)
          .extracting(StatisticsServiceImplTest::errorCodeOf)
          .isEqualTo(StatisticsErrorCode.STATISTICS_UNIVERSITY_NAME_REQUIRED);
      verify(statisticsRepository, never()).searchBoothSales(any(), any());
    }

    @Test
    @DisplayName("학교명·부스명의 앞뒤 공백을 제거하고 !, %, _를 이스케이프해 조회한다")
    void trimsAndEscapesKeywords() {
      given(statisticsRepository.searchBoothSales("서경!%대", "소프!_트!!웨어")).willReturn(List.of());

      statisticsService.searchBoothSales("  서경%대  ", " 소프_트!웨어 ");

      verify(statisticsRepository).searchBoothSales("서경!%대", "소프!_트!!웨어");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("부스명이 없거나 공백뿐이면 부스명 조건 없이 조회한다")
    void searchesWithoutBoothName_whenBlank(String boothName) {
      given(statisticsRepository.searchBoothSales("서경", null)).willReturn(List.of());

      statisticsService.searchBoothSales("서경", boothName);

      verify(statisticsRepository).searchBoothSales("서경", null);
    }

    @Test
    @DisplayName("결과의 순위와 평균 주문 금액을 응답으로 옮긴다")
    void mapsRankAndAverage() {
      given(statisticsRepository.searchBoothSales("서경", null))
          .willReturn(List.of(boothRow(37, 9000L, 2L)));

      List<BoothSalesResponse> result = statisticsService.searchBoothSales("서경", null);

      assertThat(result)
          .extracting(BoothSalesResponse::getRank, BoothSalesResponse::getAverageOrderAmount)
          .containsExactly(tuple(37L, 4500L));
    }
  }

  @Nested
  @DisplayName("getBoothMenuRanking은")
  class GetBoothMenuRanking {

    @Test
    @DisplayName("학교명이 공백뿐이면 STATISTICS_UNIVERSITY_NAME_REQUIRED를 던진다")
    void throwsUniversityNameRequired_whenBlank() {
      assertThatThrownBy(() -> statisticsService.getBoothMenuRanking("  ", null))
          .isInstanceOf(CustomException.class)
          .extracting(StatisticsServiceImplTest::errorCodeOf)
          .isEqualTo(StatisticsErrorCode.STATISTICS_UNIVERSITY_NAME_REQUIRED);
    }

    @Test
    @DisplayName("정제한 검색어로 조회하고 결과를 응답으로 옮긴다")
    void searchesWithNormalizedKeywordsAndMapsRows() {
      given(statisticsRepository.findBoothMenuRanking("서경", "소프트웨어"))
          .willReturn(List.of(menuRow(1, "닭꼬치"), menuRow(2, "떡볶이")));

      List<BoothMenuSalesResponse> result =
          statisticsService.getBoothMenuRanking(" 서경 ", " 소프트웨어 ");

      assertThat(result)
          .extracting(
              BoothMenuSalesResponse::getRank,
              BoothMenuSalesResponse::getBoothId,
              BoothMenuSalesResponse::getBoothMenuId,
              BoothMenuSalesResponse::getUniversityName,
              BoothMenuSalesResponse::getDepartmentName,
              BoothMenuSalesResponse::getMenuName,
              BoothMenuSalesResponse::getQuantity,
              BoothMenuSalesResponse::getSales)
          .containsExactly(
              tuple(1L, 1L, 11L, "서경대학교", "소프트웨어학과", "닭꼬치", 5L, 20000L),
              tuple(2L, 1L, 12L, "서경대학교", "소프트웨어학과", "떡볶이", 5L, 20000L));
    }
  }

  @Nested
  @DisplayName("getTopMenus는")
  class GetTopMenus {

    @Test
    @DisplayName("QUANTITY면 판매량 순 쿼리를 limit 10으로 호출한다")
    void usesQuantityQuery() {
      given(statisticsRepository.findTopMenusByQuantity(10)).willReturn(List.of(menuRow(1, "닭꼬치")));

      List<MenuSalesResponse> result = statisticsService.getTopMenus(MenuSortType.QUANTITY);

      assertThat(result).extracting(MenuSalesResponse::getMenuName).containsExactly("닭꼬치");
      verify(statisticsRepository, never()).findTopMenusBySales(anyInt());
    }

    @Test
    @DisplayName("SALES면 수익 순 쿼리를 limit 10으로 호출한다")
    void usesSalesQuery() {
      given(statisticsRepository.findTopMenusBySales(10)).willReturn(List.of(menuRow(1, "해물파전")));

      List<MenuSalesResponse> result = statisticsService.getTopMenus(MenuSortType.SALES);

      assertThat(result)
          .extracting(MenuSalesResponse::getRank, MenuSalesResponse::getMenuName)
          .containsExactly(tuple(1L, "해물파전"));
      verify(statisticsRepository, never()).findTopMenusByQuantity(anyInt());
    }
  }
}
