/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.global.config.property.JwtProperties;
import com.skulikelion.festival.global.security.AuthPrincipal;
import com.skulikelion.festival.global.security.jwt.internal.GeneratedRefreshTokenPayload;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtProvider {

  private final JwtProperties jwtProperties;

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String ROLE_PREFIX = "ROLE_";
  private static final String DEPARTMENT_ID_CLAIM = "departmentId";
  private static final String UNIVERSITY_ID_CLAIM = "universityId";

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  /**
   * [ Access Token 발급 메서드 ]
   *
   * <p>subject에는 {@code manager.id}(대리 키)를 담고, 자원 소유권 판정에 쓰는 {@code departmentId}와 {@code
   * universityId}를 클레임으로 함께 싣는다. 덕분에 인가 때마다 {@code managers}를 조회하지 않아도 된다.
   *
   * <p>전국 확장 이전에는 subject가 {@code Department} enum 이름이었다. 학과명이 바뀌면 발급된 토큰이 전부 무효가 되는 구조였다.
   * (ADR-0001 옵션 2)
   *
   * @param principal 인증된 요청 주체
   * @param authorities 부여된 권한 목록
   * @return 서명된 Access Token
   */
  public String generateAccessToken(
      AuthPrincipal principal, Collection<? extends GrantedAuthority> authorities) {
    Instant now = Instant.now();

    List<String> roles = authorities.stream().map(GrantedAuthority::getAuthority).toList();
    long validitySeconds = jwtProperties.getAccessTokenValidityInSeconds();
    Date issuedAt = Date.from(now);
    Date expiration = Date.from(now.plusSeconds(validitySeconds));

    return Jwts.builder()
        .subject(String.valueOf(principal.managerId()))
        .claim("type", TokenType.ACCESS_TOKEN.name())
        .claim("role", roles)
        .claim(DEPARTMENT_ID_CLAIM, principal.departmentId())
        .claim(UNIVERSITY_ID_CLAIM, principal.universityId())
        .issuedAt(issuedAt)
        .expiration(expiration)
        .signWith(getSigningKey())
        .compact();
  }

  /**
   * [ Refresh Token 발급 메서드 ]
   *
   * <p>재발급 시 Access Token을 다시 만들어야 하므로 같은 클레임을 싣는다. 그래야 재발급 경로에서 학과·대학을 다시 조회하지 않는다.
   *
   * @param principal 인증된 요청 주체
   * @return 토큰과 jti
   */
  public GeneratedRefreshTokenPayload generateRefreshToken(AuthPrincipal principal) {
    Instant now = Instant.now();

    long validitySeconds = jwtProperties.getRefreshTokenValidityInSeconds();
    Date issuedAt = Date.from(now);
    Date expiration = Date.from(now.plusSeconds(validitySeconds));
    String jti = UUID.randomUUID().toString();

    String token =
        Jwts.builder()
            .subject(String.valueOf(principal.managerId()))
            .claim("type", TokenType.REFRESH_TOKEN.name())
            .claim("role", List.of(ROLE_PREFIX + principal.role().name()))
            .claim(DEPARTMENT_ID_CLAIM, principal.departmentId())
            .claim(UNIVERSITY_ID_CLAIM, principal.universityId())
            .issuedAt(issuedAt)
            .expiration(expiration)
            .id(jti)
            .signWith(getSigningKey())
            .compact();
    return new GeneratedRefreshTokenPayload(token, jti);
  }

  public Long getManagerIdFromToken(String token) {
    return Long.valueOf(extractClaims(token).getSubject());
  }

  /**
   * [ 토큰에서 인증 주체를 복원하는 메서드 ]
   *
   * @param token Access 또는 Refresh Token
   * @return 토큰 클레임으로 복원한 {@link AuthPrincipal}
   */
  public AuthPrincipal getPrincipalFromToken(String token) {
    Claims claims = extractClaims(token);
    List<String> roles = claims.get("role", List.class);
    return new AuthPrincipal(
        Long.valueOf(claims.getSubject()),
        claims.get(DEPARTMENT_ID_CLAIM, Long.class),
        claims.get(UNIVERSITY_ID_CLAIM, Long.class),
        toRole(roles));
  }

  private Role toRole(List<String> roles) {
    return roles.stream()
        .findFirst()
        .map(role -> role.startsWith(ROLE_PREFIX) ? role.substring(ROLE_PREFIX.length()) : role)
        .map(Role::valueOf)
        .orElse(Role.USER);
  }

  public String getJtiFromToken(String token) {
    return extractClaims(token).getId();
  }

  public List<GrantedAuthority> getAuthoritiesFromToken(String token) {
    List<String> roles = extractClaims(token).get("role", List.class);
    return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
  }

  public String extractAccessToken(HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

    if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    } else if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if (TokenType.ACCESS_TOKEN.name().equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  public String extractRefreshToken(HttpServletRequest request) {
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if (TokenType.REFRESH_TOKEN.name().equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  public boolean validateToken(String token, TokenType type) {
    try {
      Claims claims = extractClaims(token);
      String tokenType = claims.get("type", String.class);
      if (!tokenType.equals(type.name())) {
        log.info("[Jwt] 일치하지 않는 토큰 타입.");
        return false;
      }
      return true;
    } catch (SecurityException | MalformedJwtException e) {
      log.info("[Jwt] 잘못된 JWT 서명입니다.");
    } catch (ExpiredJwtException e) {
      log.info("[Jwt] 만료된 JWT 토큰입니다.");
      throw e;
    } catch (UnsupportedJwtException e) {
      log.info("[Jwt] 지원되지 않는 JWT 토큰입니다.");
    } catch (IllegalArgumentException e) {
      log.warn("[Jwt] JWT 토큰이 null 또는 비어있습니다.");
    } catch (JwtException e) {
      log.info("[Jwt] JWT 토큰이 잘못되었습니다.");
    }
    return false;
  }

  private Claims extractClaims(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }
}
