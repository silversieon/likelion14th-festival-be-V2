/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.infra.redis;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.skulikelion.festival.global.config.property.JwtProperties;

import io.jsonwebtoken.MalformedJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenRepository {

  private final RedisTemplate<String, String> redisTemplate;
  private final JwtProperties jwtProperties;

  public void saveRefreshToken(String token, String jti) {
    String redisKey = jwtProperties.getRefreshTokenPrefix() + jti;
    redisTemplate
        .opsForValue()
        .set(redisKey, token, jwtProperties.getRefreshTokenValidityInSeconds(), TimeUnit.SECONDS);
  }

  public void deleteRefreshToken(String jti) {
    redisTemplate.delete(jwtProperties.getRefreshTokenPrefix() + jti);
  }

  public void validateStoredRefreshToken(String refreshToken, String jti) {
    String storedRefreshToken =
        redisTemplate.opsForValue().get(jwtProperties.getRefreshTokenPrefix() + jti);
    if (!refreshToken.equals(storedRefreshToken)) {
      throw new MalformedJwtException("[RefreshTokenRepository] Invalid refresh token");
    }
  }
}
