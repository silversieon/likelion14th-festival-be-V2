/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.global.config.property.JwtProperties;
import com.skulikelion.festival.global.security.AuthPrincipal;

/**
 * {@link JwtProvider} 테스트입니다. (LLD-0001 13.1 D그룹)
 *
 * @since 2026.09.14
 */
@DisplayName("JwtProvider는")
class JwtProviderTest {

  private static final String SECRET = "test-secret-key-for-jwt-provider-unit-test-1234567890";

  private final JwtProvider jwtProvider =
      new JwtProvider(new JwtProperties(SECRET, 3600L, 7200L, false, "Lax", "refresh:"));

  private final AuthPrincipal principal = new AuthPrincipal(1024L, 12L, 1L, Role.BOOTH_MANAGER);

  private final List<GrantedAuthority> authorities =
      List.of(new SimpleGrantedAuthority("ROLE_BOOTH_MANAGER"));

  @Test
  @DisplayName("Access Token의 subject로 관리자 식별자를 담는다")
  void generateAccessToken_putsManagerIdInSubject() {
    String accessToken = jwtProvider.generateAccessToken(principal, authorities);

    assertThat(jwtProvider.getManagerIdFromToken(accessToken)).isEqualTo(1024L);
  }

  @Test
  @DisplayName("발급한 Access Token에서 AuthPrincipal을 그대로 복원한다")
  void getPrincipalFromToken_restoresPrincipal() {
    String accessToken = jwtProvider.generateAccessToken(principal, authorities);

    AuthPrincipal restored = jwtProvider.getPrincipalFromToken(accessToken);

    assertThat(restored).isEqualTo(principal);
  }

  @Test
  @DisplayName("발급한 Refresh Token에서도 AuthPrincipal을 복원한다")
  void getPrincipalFromToken_restoresPrincipalFromRefreshToken() {
    String refreshToken = jwtProvider.generateRefreshToken(principal).token();

    AuthPrincipal restored = jwtProvider.getPrincipalFromToken(refreshToken);

    assertThat(restored).isEqualTo(principal);
  }

  @Test
  @DisplayName("Refresh Token을 Access Token으로 검증하면 false를 반환한다")
  void validateToken_isFalse_whenTokenTypeDiffers() {
    String refreshToken = jwtProvider.generateRefreshToken(principal).token();

    assertThat(jwtProvider.validateToken(refreshToken, TokenType.ACCESS_TOKEN)).isFalse();
  }
}
