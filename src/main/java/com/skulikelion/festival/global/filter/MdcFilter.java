/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.filter;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 사용자 요청 시 Spring 내부에서 거치는 Mdc 필터입니다. (클라이언트 IP, TraceId 추적용)
 *
 * @since 2026.02.09
 * @author Keum Si Eon
 */
@Component
public class MdcFilter extends OncePerRequestFilter {

  private static final String TRACE_ID = "traceId";
  private static final String CLIENT_IP = "clientIp";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String traceId = UUID.randomUUID().toString().substring(0, 8);
      MDC.put(TRACE_ID, traceId);
      MDC.put(CLIENT_IP, extractClientIp(request));

      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }

  private String extractClientIp(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isEmpty()) {
      return xff.split(",")[0].trim();
    }

    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isEmpty()) {
      return realIp;
    }

    return request.getRemoteAddr();
  }
}
