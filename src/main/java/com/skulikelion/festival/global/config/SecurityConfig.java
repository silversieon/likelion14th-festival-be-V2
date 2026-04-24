/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.skulikelion.festival.global.common.BaseResponse;
import com.skulikelion.festival.global.filter.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CorsConfig corsConfig;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final ObjectMapper objectMapper;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    configureFilters(http);
    configureExceptionHandling(http);
    configureAuthorization(http);
    return http.build();
  }

  /** 필터와 기본 설정 */
  private void configureFilters(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
  }

  /** 예외 처리: 권한 부족 처리 */
  private void configureExceptionHandling(HttpSecurity http) throws Exception {
    http.exceptionHandling(
        e ->
            e.accessDeniedHandler(this::handleAccessDenied)
                .authenticationEntryPoint(this::handleUnauthorizedOrForbidden));
  }

  private void handleUnauthorizedOrForbidden(
      HttpServletRequest request,
      HttpServletResponse response,
      org.springframework.security.core.AuthenticationException e)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    BaseResponse<Object> body = BaseResponse.error(401, "인증이 필요합니다.");

    log.warn("[Auth] Security: 인증 필요: {}, {}", request.getMethod(), request.getRequestURI());
    response.getWriter().write(objectMapper.writeValueAsString(body));
    response.getWriter().flush();
  }

  private void handleAccessDenied(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    BaseResponse<Object> baseResponse = BaseResponse.error(403, "접근 권한이 없습니다.");

    log.warn("[Auth] Security: 권한 부족: {}, {}", request.getMethod(), request.getRequestURI());
    response.getWriter().write(objectMapper.writeValueAsString(baseResponse));
    response.getWriter().flush();
  }

  /** 권한 설정 */
  private void configureAuthorization(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
        auth ->
            auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                .permitAll()
                .requestMatchers("/error")
                .permitAll()
                .requestMatchers("/actuator/health", "/actuator/prometheus")
                .permitAll()
                .requestMatchers("/api/auth/**")
                .permitAll()
                .anyRequest()
                .permitAll());
  }

  /** 비밀번호 인코더 Bean */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** 인증 관리자 Bean */
  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }
}
