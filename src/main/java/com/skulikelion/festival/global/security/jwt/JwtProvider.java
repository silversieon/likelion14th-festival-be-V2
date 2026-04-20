/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.skulikelion.festival.global.config.property.JwtProperties;
import com.skulikelion.festival.global.infra.redis.RefreshTokenRepository;
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
  private final RefreshTokenRepository refreshTokenRepository;

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(Authentication authentication) {
    Instant now = Instant.now();

    String username = authentication.getName();
    long validitySeconds = jwtProperties.getAccessTokenValidityInSeconds();
    Date issuedAt = Date.from(now);
    Date expiration = Date.from(now.plusSeconds(validitySeconds));

    return Jwts.builder()
        .subject(username)
        .claim("type", TokenType.ACCESS_TOKEN.name())
        .issuedAt(issuedAt)
        .expiration(expiration)
        .signWith(getSigningKey())
        .compact();
  }

  public GeneratedRefreshTokenPayload generateRefreshToken(Authentication authentication) {
    Instant now = Instant.now();

    String username = authentication.getName();
    long validitySeconds = jwtProperties.getRefreshTokenValidityInSeconds();
    Date issuedAt = Date.from(now);
    Date expiration = Date.from(now.plusSeconds(validitySeconds));
    String jti = UUID.randomUUID().toString();
    String token =
        Jwts.builder()
            .subject(username)
            .claim("type", TokenType.REFRESH_TOKEN.name())
            .issuedAt(issuedAt)
            .expiration(expiration)
            .id(jti)
            .signWith(getSigningKey())
            .compact();
    return new GeneratedRefreshTokenPayload(token, jti);
  }

  public String getUsernameFromToken(String token) {
    return Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }

  public String getJtiFromRefreshToken(String refreshToken) {
    Claims claims =
        Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(refreshToken)
            .getPayload();

    return claims.getId();
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
      validateTokenType(token, type);
      Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
      return true;
    } catch (SecurityException | MalformedJwtException e) {
      log.info("[Jwt] 잘못된 JWT 서명입니다.");
    } catch (ExpiredJwtException e) {
      log.info("[Jwt] 만료된 JWT 토큰입니다.");
      throw e;
    } catch (UnsupportedJwtException e) {
      log.info("[Jwt] 지원되지 않는 JWT 토큰입니다.");
    } catch (IllegalArgumentException | JwtException e) {
      log.info("[Jwt] JWT 토큰이 잘못되었습니다.");
      throw e;
    }
    return false;
  }

  private void validateTokenType(String token, TokenType tokenType) throws JwtException {
    if (!getTokenTypeFromToken(token).equals(tokenType.name())) {
      throw new JwtException("일치하지 않는 토큰 타입");
    }
  }

  private String getTokenTypeFromToken(String token) {
    return Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .get("type", String.class);
  }
}
