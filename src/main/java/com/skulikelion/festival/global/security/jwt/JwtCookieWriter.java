/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.security.jwt;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.skulikelion.festival.global.config.property.JwtProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 로그인, 로그아웃 시 반환할 쿠키를 작성하는 클래스입니다.
 *
 * @since 2026.01.17
 * @see
 * @author Keum Si Eon
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtCookieWriter {

  private final JwtProperties jwtProperties;

  public ResponseCookie addAccessTokenToCookie(String accessToken) {
    return ResponseCookie.from(TokenType.ACCESS_TOKEN.name(), accessToken)
        .httpOnly(true)
        .secure(jwtProperties.isSecure())
        .sameSite(jwtProperties.getSameSite())
        .path("/")
        .maxAge(jwtProperties.getAccessTokenValidityInSeconds())
        .build();
  }

  public ResponseCookie addRefreshTokenToCookie(String refreshToken) {
    return ResponseCookie.from(TokenType.REFRESH_TOKEN.name(), refreshToken)
        .httpOnly(true)
        .secure(jwtProperties.isSecure())
        .sameSite(jwtProperties.getSameSite())
        .path("/")
        .maxAge(jwtProperties.getRefreshTokenValidityInSeconds())
        .build();
  }

  public ResponseCookie removeTokenFromCookie(TokenType tokenType) {
    return ResponseCookie.from(tokenType.name(), null)
        .httpOnly(true)
        .secure(jwtProperties.isSecure())
        .sameSite(jwtProperties.getSameSite())
        .path("/")
        .maxAge(0)
        .build();
  }
}
