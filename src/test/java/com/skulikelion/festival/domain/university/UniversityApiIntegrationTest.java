/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.university;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import jakarta.servlet.http.Cookie;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.skulikelion.festival.domain.support.IntegrationTestSupport;
import com.skulikelion.festival.domain.university.entity.Department;
import com.skulikelion.festival.domain.university.entity.University;
import com.skulikelion.festival.domain.university.enums.Region;
import com.skulikelion.festival.domain.university.repository.DepartmentRepository;
import com.skulikelion.festival.domain.university.repository.UniversityRepository;
import com.skulikelion.festival.global.config.property.JwtProperties;
import com.skulikelion.festival.global.security.jwt.TokenType;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * 학교명 검색·학과 목록 조회 API를 실제 MySQL과 Spring Security 필터 체인 위에서 검증합니다. (LLD-0002 13.2)
 *
 * <p>각 테스트는 서로 다른 학교명을 써서 {@code UNIQUE(name, region)} 충돌 없이 독립적으로 동작한다.
 *
 * <p>⚠️ Testcontainers를 쓰므로 Docker Desktop이 실행 중이어야 한다.
 *
 * @since 2026.09.16
 */
@DisplayName("대학·학과 조회 API는")
class UniversityApiIntegrationTest extends IntegrationTestSupport {

  @Autowired private WebApplicationContext context;
  @Autowired private UniversityRepository universityRepository;
  @Autowired private DepartmentRepository departmentRepository;
  @Autowired private JwtProperties jwtProperties;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  private University saveUniversity(String name) {
    return universityRepository.saveAndFlush(
        University.builder().name(name).region(Region.JEJU).build());
  }

  private void saveDepartment(University university, String name) {
    departmentRepository.saveAndFlush(
        Department.builder().university(university).name(name).build());
  }

  private String expiredAccessToken() {
    Instant past = Instant.now().minusSeconds(3600);
    return Jwts.builder()
        .subject("1")
        .claim("type", TokenType.ACCESS_TOKEN.name())
        .issuedAt(Date.from(past.minusSeconds(60)))
        .expiration(Date.from(past))
        .signWith(Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)))
        .compact();
  }

  @Test
  @DisplayName("학교명을 앞부분부터 일치하는 것만 찾는다")
  void searchesByPrefixOnly() throws Exception {
    saveUniversity("검증앞글자대학교");

    mockMvc
        .perform(get("/api/universities").param("name", "검증앞"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].universityName").value("검증앞글자대학교"))
        .andExpect(jsonPath("$.data[0].universityId").isNumber());

    mockMvc
        .perform(get("/api/universities").param("name", "글자대학교"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.data[*].universityName", Matchers.not(Matchers.hasItem("검증앞글자대학교"))));
  }

  @Test
  @DisplayName("검색 결과를 학교명 오름차순으로 반환한다")
  void returnsUniversitiesOrderedByName() throws Exception {
    saveUniversity("검증정렬다대학교");
    saveUniversity("검증정렬가대학교");
    saveUniversity("검증정렬나대학교");

    mockMvc
        .perform(get("/api/universities").param("name", "검증정렬"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.data[*].universityName", Matchers.contains("검증정렬가대학교", "검증정렬나대학교", "검증정렬다대학교")));
  }

  @Test
  @DisplayName("검색어의 %는 와일드카드가 아니라 문자로 취급한다")
  void treatsPercentAsLiteral() throws Exception {
    saveUniversity("검증와일드카드대학교");

    mockMvc
        .perform(get("/api/universities").param("name", "검증%"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(0));
  }

  @Test
  @DisplayName("해당 학교의 학과만 학과명 오름차순으로 반환한다")
  void returnsOnlyDepartmentsOfUniversityOrderedByName() throws Exception {
    University target = saveUniversity("검증학과대상대학교");
    University other = saveUniversity("검증학과다른대학교");
    saveDepartment(target, "영문학과");
    saveDepartment(target, "경영학과");
    saveDepartment(other, "경영학과");
    saveDepartment(other, "건축학과");

    mockMvc
        .perform(get("/api/universities/{universityId}/departments", target.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].departmentName", Matchers.contains("경영학과", "영문학과")))
        .andExpect(jsonPath("$.data[0].departmentId").isNumber());
  }

  @Test
  @DisplayName("존재하지 않는 학교의 학과를 조회하면 404다")
  void returnsNotFound_whenUniversityMissing() throws Exception {
    mockMvc
        .perform(get("/api/universities/{universityId}/departments", Long.MAX_VALUE))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("해당 대학을 찾을 수 없습니다."));
  }

  @Test
  @DisplayName("검색어 없이 호출하면 400이다")
  void returnsBadRequest_whenNameMissing() throws Exception {
    mockMvc
        .perform(get("/api/universities"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("검색할 학교명을 입력해주세요."));
  }

  @Test
  @DisplayName("만료된 ACCESS_TOKEN 쿠키가 있어도 401 없이 조회된다")
  void ignoresExpiredAccessTokenCookie() throws Exception {
    University university = saveUniversity("검증만료토큰대학교");
    Cookie expired = new Cookie(TokenType.ACCESS_TOKEN.name(), expiredAccessToken());

    mockMvc
        .perform(get("/api/universities").param("name", "검증만료토큰").cookie(expired))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/universities/{universityId}/departments", university.getId()).cookie(expired))
        .andExpect(status().isOk());
  }
}
