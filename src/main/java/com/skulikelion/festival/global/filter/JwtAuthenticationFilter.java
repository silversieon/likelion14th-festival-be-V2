/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.skulikelion.festival.domain.auth.exception.AuthErrorCode;
import com.skulikelion.festival.global.common.BaseResponse;
import com.skulikelion.festival.global.security.jwt.JwtProvider;
import com.skulikelion.festival.global.security.jwt.TokenType;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * 사용자 요청 시 Spring 내부에서 거치는 인증 필터입니다.
 *
 * @since 2026.01.19
 * @see UserDetailsService
 * @see com.skulikelion.festival.global.security.CustomUserDetailsService
 * @see com.skulikelion.festival.global.security.CustomUserDetails
 * @author Keum Si Eon
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;
  private final UserDetailsService userDetailsService;
  private static final AntPathMatcher pathMatcher = new AntPathMatcher();
  private final ObjectMapper objectMapper;

  /**
   * [ 인증 필터를 거치지 않는 경로 요청 확인 메서드 ] 만약 patchMatcher에 해당하는 엔드포인트라면 필터 거치기 X
   *
   * @param request 사용자 요청
   * @return patchMatcher와 match 되는지 여부
   */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return pathMatcher.match("/api/auth/refresh", uri) || pathMatcher.match("/api/auth/login", uri);
  }

  /**
   * [ 사용자 요청 시 AccessToken을 통한 인증 절차 필터 ]
   *
   * @param request 사용자 요청 객체
   * @param response 서버 응답 객체
   * @param filterChain 필터 체인
   * @throws ServletException 서블릿 예외
   * @throws IOException 입출력 예외
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if (shouldNotFilter(request)) filterChain.doFilter(request, response);
    if ("/error".equals(request.getRequestURI())) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      String accessToken = jwtProvider.extractAccessToken(request);

      if (accessToken != null && jwtProvider.validateToken(accessToken, TokenType.ACCESS_TOKEN)) {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
          String username = jwtProvider.getUsernameFromToken(accessToken);
          UserDetails userDetails = userDetailsService.loadUserByUsername(username);

          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                  userDetails, null, userDetails.getAuthorities());
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      }
      filterChain.doFilter(request, response);
    } catch (ExpiredJwtException e) {
      SecurityContextHolder.clearContext();
      log.info("[Auth] Security: 만료된 JWT 액세스 토큰 입력 확인, 리프레시 필요");
      writeAuthErrorResponse(response, AuthErrorCode.EXPIRED_ACCESS_TOKEN);
    } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
      SecurityContextHolder.clearContext();
      log.warn("[Auth] Security: 유효하지 않은 JWT 토큰 입력 확인, 주의 필요 - {}", e.getClass().getSimpleName());
      writeAuthErrorResponse(response, AuthErrorCode.UNAUTHORIZED_TOKEN);
    }
  }

  /**
   * [ JwtAuthenticationFilter 도중 예외 처리 메서드 ]
   *
   * @param response 서버 응답 객체
   * @param errorCode 인증 에러 코드
   * @throws IOException 입출력 예외
   */
  private void writeAuthErrorResponse(HttpServletResponse response, AuthErrorCode errorCode)
      throws IOException {
    if (response.isCommitted()) return;

    response.setStatus(errorCode.getStatus().value());
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    BaseResponse<Object> baseResponse =
        BaseResponse.error(errorCode.getStatus().value(), errorCode.getMessage());
    response.getWriter().write(objectMapper.writeValueAsString(baseResponse));
    response.getWriter().flush();
  }
}
