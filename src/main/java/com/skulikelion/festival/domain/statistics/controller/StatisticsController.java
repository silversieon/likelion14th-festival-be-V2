/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.statistics.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skulikelion.festival.domain.statistics.dto.response.BoothMenuSalesResponse;
import com.skulikelion.festival.domain.statistics.dto.response.BoothSalesResponse;
import com.skulikelion.festival.domain.statistics.dto.response.DailySalesResponse;
import com.skulikelion.festival.domain.statistics.dto.response.HourlySalesResponse;
import com.skulikelion.festival.domain.statistics.dto.response.MenuSalesResponse;
import com.skulikelion.festival.domain.statistics.dto.response.SalesSummaryResponse;
import com.skulikelion.festival.domain.statistics.enums.MenuSortType;
import com.skulikelion.festival.domain.statistics.service.StatisticsService;
import com.skulikelion.festival.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 운영진 통계 조회 Controller 입니다.
 *
 * <p>운영진 통계 화면(매출 / 부스 / 메뉴 탭)에 쓰이며 {@code ADMIN}만 호출할 수 있다. 집계 대상은 완료된 주문뿐이고, 기준 시각은 주문 완료 시각이다.
 * (LLD-0005)
 *
 * <p>필수 검색 조건도 {@code required = false}로 받아 서비스에서 400으로 검증한다. 필수 {@code @RequestParam} 누락은 {@code
 * GlobalExceptionHandler}가 500으로 처리하기 때문이다.
 *
 * @see StatisticsService
 * @since 2026.10.10
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/statistics")
@Tag(name = "Statistics", description = "운영진 매출·부스·메뉴 통계 조회 기능을 제공하는 API")
public class StatisticsController {

  private final StatisticsService statisticsService;

  @Operation(
      summary = "[ 운영진 | 토큰 O | 총 주문 수·총 수익 조회 ]",
      description =
          """
          **Returns**  \n
          totalOrders: 완료된 주문 수 \n
          totalSales: 완료된 주문의 총 주문 금액 \n
          """)
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/sales/summary")
  public ResponseEntity<BaseResponse<SalesSummaryResponse>> getSalesSummary() {
    SalesSummaryResponse response = statisticsService.getSalesSummary();
    return ResponseEntity.status(200).body(BaseResponse.success(200, "총 매출 조회에 성공했습니다.", response));
  }

  @Operation(
      summary = "[ 운영진 | 토큰 O | 일별 매출 추이 조회 ]",
      description =
          """
          **Parameters**  \n
          startDate: 조회 시작일 (포함, 필수) \n
          endDate: 조회 종료일 (포함, 필수) \n
          \n
          **Returns**  \n
          date: 일자 (주문이 없는 날도 0으로 포함) \n
          totalSales: 그날 완료된 주문의 총 주문 금액 \n
          orderCount: 그날 완료된 주문 수 \n
          """)
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/sales/daily")
  public ResponseEntity<BaseResponse<List<DailySalesResponse>>> getDailySales(
      @Parameter(description = "조회 시작일 (포함)", example = "2026-10-04")
          @RequestParam(required = false)
          @DateTimeFormat(iso = ISO.DATE)
          LocalDate startDate,
      @Parameter(description = "조회 종료일 (포함)", example = "2026-10-10")
          @RequestParam(required = false)
          @DateTimeFormat(iso = ISO.DATE)
          LocalDate endDate) {
    List<DailySalesResponse> responses = statisticsService.getDailySales(startDate, endDate);
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "일별 매출 조회에 성공했습니다.", responses));
  }

  @Operation(
      summary = "[ 운영진 | 토큰 O | 시간대별 매출 추이 조회 ]",
      description =
          """
          **Parameters**  \n
          startDate: 조회 시작일 (포함) — 종료일과 함께 생략하면 전체 기간 \n
          endDate: 조회 종료일 (포함) — 시작일과 함께 생략하면 전체 기간 \n
          \n
          **Returns**  \n
          hour: 시각 (11 ~ 23, 13개 구간 항상 반환) \n
          totalSales: 해당 시각 구간의 총 주문 금액 \n
          orderCount: 해당 시각 구간의 주문 수 \n
          """)
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/sales/hourly")
  public ResponseEntity<BaseResponse<List<HourlySalesResponse>>> getHourlySales(
      @Parameter(description = "조회 시작일 (포함)", example = "2026-10-09")
          @RequestParam(required = false)
          @DateTimeFormat(iso = ISO.DATE)
          LocalDate startDate,
      @Parameter(description = "조회 종료일 (포함)", example = "2026-10-10")
          @RequestParam(required = false)
          @DateTimeFormat(iso = ISO.DATE)
          LocalDate endDate) {
    List<HourlySalesResponse> responses = statisticsService.getHourlySales(startDate, endDate);
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "시간대별 매출 조회에 성공했습니다.", responses));
  }

  @Operation(
      summary = "[ 운영진 | 토큰 O | 부스별 매출 TOP 10 조회 ]",
      description =
          """
          **Returns**  \n
          rank: 전체 부스 기준 매출 순위 \n
          boothId: 부스 식별자 \n
          universityName: 학교명 \n
          departmentName: 학과명 (부스명) \n
          totalSales: 총 매출 \n
          averageOrderAmount: 평균 주문 금액 (반올림) \n
          orderCount: 총 주문 건수 \n
          totalQuantity: 총 판매 메뉴 수 \n
          """)
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/booths/top")
  public ResponseEntity<BaseResponse<List<BoothSalesResponse>>> getTopBoothSales() {
    List<BoothSalesResponse> responses = statisticsService.getTopBoothSales();
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "부스별 매출 TOP 10 조회에 성공했습니다.", responses));
  }

  @Operation(
      summary = "[ 운영진 | 토큰 O | 부스별 매출 검색 ]",
      description =
          """
          **Parameters**  \n
          universityName: 학교명 (부분 일치, 필수) \n
          boothName: 부스(학과)명 (부분 일치, 선택) \n
          \n
          **Returns**  \n
          부스별 매출 TOP 10과 같은 필드. rank는 검색 결과 안이 아니라 전체 부스 기준 순위 \n
          """)
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/booths/search")
  public ResponseEntity<BaseResponse<List<BoothSalesResponse>>> searchBoothSales(
      @Parameter(description = "학교명 (부분 일치)", example = "서경대학교") @RequestParam(required = false)
          String universityName,
      @Parameter(description = "부스(학과)명 (부분 일치)", example = "소프트웨어학과")
          @RequestParam(required = false)
          String boothName) {
    List<BoothSalesResponse> responses =
        statisticsService.searchBoothSales(universityName, boothName);
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "부스별 매출 검색에 성공했습니다.", responses));
  }

  @Operation(
      summary = "[ 운영진 | 토큰 O | 부스별 인기 메뉴 순위 조회 ]",
      description =
          """
          **Parameters**  \n
          universityName: 학교명 (부분 일치, 필수) \n
          boothName: 부스(학과)명 (부분 일치, 선택) \n
          \n
          **Returns**  \n
          rank: 부스 안에서의 순위 \n
          boothId: 부스 식별자 \n
          boothMenuId: 메뉴 식별자 \n
          universityName: 학교명 \n
          departmentName: 학과명 (부스명) \n
          menuName: 메뉴명 \n
          quantity: 주문 수 \n
          sales: 총 수익 \n
          """)
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/booths/menus")
  public ResponseEntity<BaseResponse<List<BoothMenuSalesResponse>>> getBoothMenuRanking(
      @Parameter(description = "학교명 (부분 일치)", example = "서경대학교") @RequestParam(required = false)
          String universityName,
      @Parameter(description = "부스(학과)명 (부분 일치)", example = "소프트웨어학과")
          @RequestParam(required = false)
          String boothName) {
    List<BoothMenuSalesResponse> responses =
        statisticsService.getBoothMenuRanking(universityName, boothName);
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "부스별 인기 메뉴 순위 조회에 성공했습니다.", responses));
  }

  @Operation(
      summary = "[ 운영진 | 토큰 O | 메뉴 TOP 10 조회 ]",
      description =
          """
          **Parameters**  \n
          sortType: QUANTITY(판매순, 기본값) | SALES(수익순) \n
          \n
          **Returns**  \n
          rank: 순위 \n
          boothId: 부스 식별자 \n
          boothMenuId: 메뉴 식별자 \n
          universityName: 학교명 \n
          departmentName: 학과명 (부스명) \n
          menuName: 메뉴명 \n
          quantity: 판매량 \n
          sales: 총 수익 \n
          """)
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/menus/top")
  public ResponseEntity<BaseResponse<List<MenuSalesResponse>>> getTopMenus(
      @Parameter(description = "정렬 기준", example = "QUANTITY")
          @RequestParam(defaultValue = "QUANTITY")
          MenuSortType sortType) {
    List<MenuSalesResponse> responses = statisticsService.getTopMenus(sortType);
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "메뉴 TOP 10 조회에 성공했습니다.", responses));
  }
}
