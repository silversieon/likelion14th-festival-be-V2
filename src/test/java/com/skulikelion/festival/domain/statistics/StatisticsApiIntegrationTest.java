/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.entity.BoothMenu;
import com.skulikelion.festival.domain.booth.enums.MenuCategory;
import com.skulikelion.festival.domain.booth.enums.TimeType;
import com.skulikelion.festival.domain.booth.repository.BoothMenuRepository;
import com.skulikelion.festival.domain.booth.repository.BoothRepository;
import com.skulikelion.festival.domain.support.IntegrationTestSupport;
import com.skulikelion.festival.domain.university.entity.Department;
import com.skulikelion.festival.domain.university.entity.University;
import com.skulikelion.festival.domain.university.enums.Region;
import com.skulikelion.festival.domain.university.repository.DepartmentRepository;
import com.skulikelion.festival.domain.university.repository.UniversityRepository;

/**
 * 운영진 통계 API를 실제 MySQL(윈도 함수·날짜 함수)과 Spring Security 필터 체인 위에서 검증합니다. (LLD-0005 13.2)
 *
 * <p>Flyway 더미(부스·메뉴, 주문 없음)가 함께 적재되고 테스트 클래스 간에 DB를 공유하므로, 각 테스트는 고유한 학교명으로 데이터를 만들고 전체 합계는 호출 전후
 * 차이로 검증한다. 주문은 {@code completed_at}을 지정하려고 {@link JdbcTemplate}으로 넣는다.
 *
 * <p>⚠️ Testcontainers를 쓰므로 Docker Desktop이 실행 중이어야 한다.
 *
 * @since 2026.10.10
 */
@DisplayName("운영진 통계 API는")
class StatisticsApiIntegrationTest extends IntegrationTestSupport {

  @Autowired private WebApplicationContext context;
  @Autowired private UniversityRepository universityRepository;
  @Autowired private DepartmentRepository departmentRepository;
  @Autowired private BoothRepository boothRepository;
  @Autowired private BoothMenuRepository boothMenuRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  private static RequestPostProcessor admin() {
    return user("admin").roles("ADMIN");
  }

  private University saveUniversity(String name) {
    return universityRepository.saveAndFlush(
        University.builder().name(name).region(Region.JEJU).build());
  }

  private Booth saveBooth(University university, String departmentName) {
    Department department =
        departmentRepository.saveAndFlush(
            Department.builder().university(university).name(departmentName).build());
    return boothRepository.saveAndFlush(Booth.builder().department(department).build());
  }

  private BoothMenu saveMenu(Booth booth, String name, int price) {
    return boothMenuRepository.saveAndFlush(
        BoothMenu.builder()
            .booth(booth)
            .nameKo(name)
            .price(price)
            .timeType(TimeType.ALL)
            .category(MenuCategory.MAIN)
            .build());
  }

  private record Item(BoothMenu menu, int quantity) {}

  /** 주문 한 건과 항목들을 넣는다. 총 주문 금액은 항목 합계다. */
  private void saveOrder(String status, LocalDateTime completedAt, Item... items) {
    int total = 0;
    for (Item item : items) {
      total += item.menu().getPrice() * item.quantity();
    }
    int totalOrderPrice = total;
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        con -> {
          PreparedStatement ps =
              con.prepareStatement(
                  "INSERT INTO orders (customer_name, customer_phone_number, total_order_price,"
                      + " completed_at, order_status, created_at, modified_at)"
                      + " VALUES ('테스트', '010-0000-0000', ?, ?, ?, NOW(6), NOW(6))",
                  Statement.RETURN_GENERATED_KEYS);
          ps.setInt(1, totalOrderPrice);
          ps.setTimestamp(2, completedAt == null ? null : Timestamp.valueOf(completedAt));
          ps.setString(3, status);
          return ps;
        },
        keyHolder);
    long orderId = Objects.requireNonNull(keyHolder.getKey()).longValue();
    for (Item item : items) {
      jdbcTemplate.update(
          "INSERT INTO order_items (quantity, menu_price, total_order_item_price, order_id,"
              + " booth_menu_id) VALUES (?, ?, ?, ?, ?)",
          item.quantity(),
          item.menu().getPrice(),
          item.menu().getPrice() * item.quantity(),
          orderId,
          item.menu().getId());
    }
  }

  private void saveCompletedOrder(LocalDateTime completedAt, Item... items) {
    saveOrder("COMPLETED", completedAt, items);
  }

  private long readLong(String json, String path) {
    return ((Number) JsonPath.read(json, path)).longValue();
  }

  @Test
  @DisplayName("ADMIN이 아니면 403이다")
  void returnsForbidden_whenNotAdmin() throws Exception {
    mockMvc
        .perform(get("/api/statistics/sales/summary").with(user("manager").roles("BOOTH_MANAGER")))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("총계는 완료된 주문만 센다")
  void summaryCountsCompletedOrdersOnly() throws Exception {
    String before =
        mockMvc
            .perform(get("/api/statistics/sales/summary").with(admin()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Booth booth = saveBooth(saveUniversity("통계총계대학교"), "총계학과");
    BoothMenu menu = saveMenu(booth, "총계메뉴", 5000);
    LocalDateTime at = LocalDateTime.of(2001, 1, 1, 12, 0);
    saveCompletedOrder(at, new Item(menu, 1));
    saveCompletedOrder(at, new Item(menu, 2));
    saveOrder("WAITING", null, new Item(menu, 3));
    saveOrder("CANCELED", null, new Item(menu, 4));

    String after =
        mockMvc
            .perform(get("/api/statistics/sales/summary").with(admin()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(readLong(after, "$.data.totalOrders") - readLong(before, "$.data.totalOrders"))
        .isEqualTo(2L);
    assertThat(readLong(after, "$.data.totalSales") - readLong(before, "$.data.totalSales"))
        .isEqualTo(15000L);
  }

  @Test
  @DisplayName("일별 추이는 주문 없는 날을 0으로 채우고 자정 경계의 주문을 올바른 날에 넣는다")
  void dailyFillsZeroAndRespectsMidnight() throws Exception {
    Booth booth = saveBooth(saveUniversity("통계일별대학교"), "일별학과");
    BoothMenu menu = saveMenu(booth, "일별메뉴", 1000);
    saveCompletedOrder(LocalDateTime.of(2002, 2, 1, 23, 59, 59, 999_999_000), new Item(menu, 1));
    saveCompletedOrder(LocalDateTime.of(2002, 2, 2, 0, 0), new Item(menu, 2));
    saveCompletedOrder(LocalDateTime.of(2002, 2, 4, 12, 0), new Item(menu, 3));
    saveOrder("CANCELED", LocalDateTime.of(2002, 2, 4, 13, 0), new Item(menu, 9));

    mockMvc
        .perform(
            get("/api/statistics/sales/daily")
                .param("startDate", "2002-02-01")
                .param("endDate", "2002-02-04")
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.data[*].date",
                Matchers.contains("2002-02-01", "2002-02-02", "2002-02-03", "2002-02-04")))
        .andExpect(jsonPath("$.data[*].totalSales", Matchers.contains(1000, 2000, 0, 3000)))
        .andExpect(jsonPath("$.data[*].orderCount", Matchers.contains(1, 1, 0, 1)));
  }

  @Test
  @DisplayName("일별 추이에서 시작일이 종료일보다 늦으면 400이다")
  void dailyReturnsBadRequest_whenStartAfterEnd() throws Exception {
    mockMvc
        .perform(
            get("/api/statistics/sales/daily")
                .param("startDate", "2002-02-05")
                .param("endDate", "2002-02-04")
                .with(admin()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("시작일은 종료일보다 늦을 수 없습니다."));
  }

  @Test
  @DisplayName("시간대별 추이는 13개 구간을 반환하고 10시대·기간 밖 주문을 제외한다")
  void hourlyReturnsThirteenBucketsWithinPeriod() throws Exception {
    Booth booth = saveBooth(saveUniversity("통계시간대학교"), "시간학과");
    BoothMenu menu = saveMenu(booth, "시간메뉴", 1000);
    saveCompletedOrder(LocalDateTime.of(2003, 3, 1, 10, 59), new Item(menu, 5));
    saveCompletedOrder(LocalDateTime.of(2003, 3, 1, 11, 0), new Item(menu, 1));
    saveCompletedOrder(LocalDateTime.of(2003, 3, 2, 11, 30), new Item(menu, 2));
    saveCompletedOrder(LocalDateTime.of(2003, 3, 2, 23, 59, 59), new Item(menu, 4));
    saveCompletedOrder(LocalDateTime.of(2003, 3, 3, 11, 0), new Item(menu, 7));

    mockMvc
        .perform(
            get("/api/statistics/sales/hourly")
                .param("startDate", "2003-03-01")
                .param("endDate", "2003-03-02")
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(13))
        .andExpect(jsonPath("$.data[0].hour").value(11))
        .andExpect(jsonPath("$.data[0].totalSales").value(3000))
        .andExpect(jsonPath("$.data[0].orderCount").value(2))
        .andExpect(jsonPath("$.data[1].totalSales").value(0))
        .andExpect(jsonPath("$.data[12].hour").value(23))
        .andExpect(jsonPath("$.data[12].totalSales").value(4000))
        .andExpect(jsonPath("$.data[12].orderCount").value(1));
  }

  @Test
  @DisplayName("시간대별 추이는 기간 없이 호출하면 전체 기간 13개 구간을 반환한다")
  void hourlyReturnsAllPeriod_whenNoDates() throws Exception {
    mockMvc
        .perform(get("/api/statistics/sales/hourly").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(13));
  }

  @Test
  @DisplayName("부스 검색은 학교명만 주면 그 학교 부스 전부를, 부스명까지 주면 그 부스만 반환한다")
  void boothSearchFiltersByUniversityAndBooth() throws Exception {
    University university = saveUniversity("통계검색대학교");
    Booth sold = saveBooth(university, "검색판매학과");
    Booth idle = saveBooth(university, "검색휴업학과");
    saveMenu(idle, "안팔린메뉴", 1000);
    BoothMenu menu = saveMenu(sold, "검색메뉴", 3000);
    LocalDateTime at = LocalDateTime.of(2004, 4, 1, 12, 0);
    saveCompletedOrder(at, new Item(menu, 1));
    saveCompletedOrder(at, new Item(menu, 2));

    mockMvc
        .perform(get("/api/statistics/booths/search").param("universityName", "통계검색").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].departmentName", Matchers.contains("검색판매학과", "검색휴업학과")))
        .andExpect(jsonPath("$.data[0].totalSales").value(9000))
        .andExpect(jsonPath("$.data[0].orderCount").value(2))
        .andExpect(jsonPath("$.data[0].averageOrderAmount").value(4500))
        .andExpect(jsonPath("$.data[0].totalQuantity").value(3))
        .andExpect(jsonPath("$.data[1].totalSales").value(0))
        .andExpect(jsonPath("$.data[1].averageOrderAmount").value(0));

    mockMvc
        .perform(
            get("/api/statistics/booths/search")
                .param("universityName", "통계검색대학교")
                .param("boothName", "휴업")
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].boothId").value(idle.getId()));
  }

  @Test
  @DisplayName("부스 검색의 순위는 TOP 10과 같은 전체 기준 순위다")
  void boothSearchRankIsGlobal() throws Exception {
    Booth booth = saveBooth(saveUniversity("통계순위대학교"), "순위학과");
    BoothMenu menu = saveMenu(booth, "고가메뉴", 900_000_000);
    saveCompletedOrder(LocalDateTime.of(2005, 5, 1, 12, 0), new Item(menu, 1));

    String top =
        mockMvc
            .perform(get("/api/statistics/booths/top").with(admin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", Matchers.lessThanOrEqualTo(10)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    List<Number> topRanks =
        JsonPath.read(top, "$.data[?(@.boothId == " + booth.getId() + ")].rank");
    assertThat(topRanks).hasSize(1);

    mockMvc
        .perform(
            get("/api/statistics/booths/search").param("universityName", "통계순위대학교").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].rank").value(topRanks.get(0).longValue()))
        .andExpect(jsonPath("$.data[0].averageOrderAmount").value(900_000_000));
  }

  @Test
  @DisplayName("부스별 메뉴 순위는 부스 안에서 판매량 순으로 매기고 팔리지 않은 메뉴를 0으로 포함한다")
  void boothMenuRankingRanksWithinBooth() throws Exception {
    University university = saveUniversity("통계메뉴대학교");
    Booth first = saveBooth(university, "가메뉴학과");
    Booth second = saveBooth(university, "나메뉴학과");
    BoothMenu firstA = saveMenu(first, "가-적게", 10000);
    BoothMenu firstB = saveMenu(first, "가-많이", 1000);
    saveMenu(first, "가-없음", 500);
    BoothMenu secondA = saveMenu(second, "나-하나", 2000);
    LocalDateTime at = LocalDateTime.of(2006, 6, 1, 12, 0);
    saveCompletedOrder(at, new Item(firstA, 1), new Item(firstB, 5));
    saveCompletedOrder(at, new Item(secondA, 2));

    mockMvc
        .perform(get("/api/statistics/booths/menus").param("universityName", "통계메뉴").with(admin()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.data[*].menuName", Matchers.contains("가-많이", "가-적게", "가-없음", "나-하나")))
        .andExpect(jsonPath("$.data[*].rank", Matchers.contains(1, 2, 3, 1)))
        .andExpect(jsonPath("$.data[*].quantity", Matchers.contains(5, 1, 0, 2)))
        .andExpect(jsonPath("$.data[*].sales", Matchers.contains(5000, 10000, 0, 4000)));

    mockMvc
        .perform(
            get("/api/statistics/booths/menus")
                .param("universityName", "통계메뉴")
                .param("boothName", "나메뉴")
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].menuName", Matchers.contains("나-하나")));
  }

  @Test
  @DisplayName("메뉴 TOP 10은 sortType에 따라 판매순·수익순으로 정렬된다")
  void topMenusSortBySortType() throws Exception {
    Booth booth = saveBooth(saveUniversity("통계상위메뉴대학교"), "메뉴탑학과");
    BoothMenu manyCheap = saveMenu(booth, "탑-많이싸게", 1);
    BoothMenu fewExpensive = saveMenu(booth, "탑-적게비싸게", 1_000_000_000);
    saveCompletedOrder(
        LocalDateTime.of(2007, 7, 1, 12, 0),
        new Item(manyCheap, 1_000_000),
        new Item(fewExpensive, 1));

    mockMvc
        .perform(get("/api/statistics/menus/top").param("sortType", "QUANTITY").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].menuName").value("탑-많이싸게"))
        .andExpect(jsonPath("$.data[0].rank").value(1))
        .andExpect(jsonPath("$.data[0].quantity").value(1_000_000));

    mockMvc
        .perform(get("/api/statistics/menus/top").param("sortType", "SALES").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].menuName").value("탑-적게비싸게"))
        .andExpect(jsonPath("$.data[0].sales").value(1_000_000_000));
  }

  @Test
  @DisplayName("학교명 없이 부스 검색하면 400이다")
  void boothSearchReturnsBadRequest_whenUniversityNameMissing() throws Exception {
    mockMvc
        .perform(get("/api/statistics/booths/search").with(admin()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("검색할 학교명을 입력해주세요."));
  }

  @Test
  @DisplayName("sortType 값이 틀리면 400이다")
  void topMenusReturnsBadRequest_whenSortTypeInvalid() throws Exception {
    mockMvc
        .perform(get("/api/statistics/menus/top").param("sortType", "PRICE").with(admin()))
        .andExpect(status().isBadRequest());
  }
}
