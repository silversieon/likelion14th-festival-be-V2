/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.statistics.service;

import java.time.LocalDate;
import java.util.List;

import com.skulikelion.festival.domain.statistics.dto.response.BoothMenuSalesResponse;
import com.skulikelion.festival.domain.statistics.dto.response.BoothSalesResponse;
import com.skulikelion.festival.domain.statistics.dto.response.DailySalesResponse;
import com.skulikelion.festival.domain.statistics.dto.response.HourlySalesResponse;
import com.skulikelion.festival.domain.statistics.dto.response.MenuSalesResponse;
import com.skulikelion.festival.domain.statistics.dto.response.SalesSummaryResponse;
import com.skulikelion.festival.domain.statistics.enums.MenuSortType;

/**
 * 운영진 통계 조회 Service interface 입니다.
 *
 * <p>모든 집계는 완료({@code COMPLETED})된 주문만, 주문 완료 시각 기준으로 한다. (LLD-0005)
 *
 * @see com.skulikelion.festival.domain.statistics.controller.StatisticsController
 * @since 2026.10.10
 */
public interface StatisticsService {

  /**
   * [ 총 주문 수·총 수익 조회 메서드 ]
   *
   * @return 전체 기간의 완료 주문 수와 총 주문 금액
   */
  SalesSummaryResponse getSalesSummary();

  /**
   * [ 일별 매출 추이 조회 메서드 ]
   *
   * @param startDate 조회 시작일 (포함, 필수)
   * @param endDate 조회 종료일 (포함, 필수)
   * @return 기간의 모든 날짜 (주문이 없는 날은 0), 날짜 오름차순
   */
  List<DailySalesResponse> getDailySales(LocalDate startDate, LocalDate endDate);

  /**
   * [ 시간대별 매출 추이 조회 메서드 ]
   *
   * @param startDate 조회 시작일 (포함). 종료일과 함께 null이면 전체 기간
   * @param endDate 조회 종료일 (포함). 시작일과 함께 null이면 전체 기간
   * @return 11시 ~ 23시 13개 구간 (주문이 없는 구간은 0), 시각 오름차순
   */
  List<HourlySalesResponse> getHourlySales(LocalDate startDate, LocalDate endDate);

  /**
   * [ 부스별 매출 TOP 10 조회 메서드 ]
   *
   * @return 완료 주문이 있는 부스 중 매출 상위 10개
   */
  List<BoothSalesResponse> getTopBoothSales();

  /**
   * [ 부스별 매출 검색 메서드 ]
   *
   * @param universityName 학교명 (부분 일치, 필수)
   * @param boothName 부스(학과)명 (부분 일치, 선택)
   * @return 일치하는 부스 전부 (순위는 전체 부스 기준), 순위 오름차순
   */
  List<BoothSalesResponse> searchBoothSales(String universityName, String boothName);

  /**
   * [ 부스별 인기 메뉴 순위 조회 메서드 ]
   *
   * @param universityName 학교명 (부분 일치, 필수)
   * @param boothName 부스(학과)명 (부분 일치, 선택)
   * @return 일치하는 부스들의 모든 메뉴 (순위는 부스 안 기준)
   */
  List<BoothMenuSalesResponse> getBoothMenuRanking(String universityName, String boothName);

  /**
   * [ 메뉴 TOP 10 조회 메서드 ]
   *
   * @param sortType 판매순 또는 수익순
   * @return 판매된 메뉴 중 상위 10개
   */
  List<MenuSalesResponse> getTopMenus(MenuSortType sortType);
}
