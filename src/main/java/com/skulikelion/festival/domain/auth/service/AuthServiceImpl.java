/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.auth.dto.request.LoginRequest;
import com.skulikelion.festival.domain.auth.dto.request.SignUpRequest;
import com.skulikelion.festival.domain.auth.dto.response.TokenResponse;
import com.skulikelion.festival.domain.auth.exception.AuthErrorCode;
import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.repository.ManagerRepository;
import com.skulikelion.festival.global.annotation.TimeTrace;
import com.skulikelion.festival.global.config.property.AuthProperties;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.infra.redis.RefreshTokenRepository;
import com.skulikelion.festival.global.security.jwt.JwtProvider;
import com.skulikelion.festival.global.security.jwt.internal.GeneratedRefreshTokenPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final JwtProvider jwtProvider;
  private final AuthenticationManager authenticationManager;
  private final RefreshTokenRepository refreshTokenRepository;
  private final UserDetailsService userDetailsService;
  private final ManagerRepository managerRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthProperties authProperties;

  @Override
  @Transactional
  public void signUp(SignUpRequest request) {
    if (!authProperties.getAdminKey().equals(request.getAdminKey())) {
      log.error("[Auth] 잘못된 어드민 키 입력 - adminKey: {}", request.getAdminKey());
      throw new CustomException(AuthErrorCode.INCORRECT_ADMIN_KEY);
    }
    if (managerRepository.findByUsername(request.getUsername()).isPresent()) {
      log.warn("[Auth] 존재하는 아이디 입력 - username: {}", request.getUsername());
      throw new CustomException(AuthErrorCode.ALREADY_EXIST_USERNAME);
    }

    String encodedPassword = passwordEncoder.encode(request.getPassword());
    Manager manager =
        Manager.builder().username(request.getUsername()).password(encodedPassword).build();
    Manager savedManager = managerRepository.save(manager);

    log.info("[Auth] 신규 사용자 회원가입 - 아이디: {}", savedManager.getUsername());
  }

  @Override
  @Transactional(readOnly = true)
  @TimeTrace(
      methodName = "로그인",
      env = {"local"})
  public TokenResponse login(LoginRequest request) {
    try {
      UsernamePasswordAuthenticationToken authenticationToken =
          new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
      Authentication authentication = authenticationManager.authenticate(authenticationToken);

      String accessToken = jwtProvider.generateAccessToken(authentication);

      GeneratedRefreshTokenPayload generatedRefreshTokenPayload =
          jwtProvider.generateRefreshToken(authentication);
      String refreshToken = generatedRefreshTokenPayload.token();
      String jti = generatedRefreshTokenPayload.jti();
      refreshTokenRepository.saveRefreshToken(refreshToken, jti);

      TokenResponse tokenResponse =
          TokenResponse.builder().accessToken(accessToken).refreshToken(refreshToken).build();

      log.info("[Auth] 사용자 로그인 성공 - 아이디: {}", request.getUsername());
      return tokenResponse;
    } catch (BadCredentialsException | UsernameNotFoundException e) {
      log.info("[Auth] 로그인 실패 - 아이디: {}", request.getUsername());
      throw new CustomException(AuthErrorCode.LOGIN_FAIL);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public TokenResponse refresh(String refreshToken) {
    String username = jwtProvider.getUsernameFromToken(refreshToken);

    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    String jti = jwtProvider.getJtiFromRefreshToken(refreshToken);
    refreshTokenRepository.validateStoredRefreshToken(refreshToken, jti);
    refreshTokenRepository.deleteRefreshToken(jti);

    String accessToken = jwtProvider.generateAccessToken(authentication);
    GeneratedRefreshTokenPayload generatedRefreshTokenPayload =
        jwtProvider.generateRefreshToken(authentication);
    refreshTokenRepository.saveRefreshToken(
        generatedRefreshTokenPayload.token(), generatedRefreshTokenPayload.jti());

    return TokenResponse.builder()
        .accessToken(accessToken)
        .refreshToken(generatedRefreshTokenPayload.token())
        .build();
  }

  @Override
  public void logout(String refreshToken) {
    String username = jwtProvider.getUsernameFromToken(refreshToken);
    String jti = jwtProvider.getJtiFromRefreshToken(refreshToken);
    refreshTokenRepository.deleteRefreshToken(jti);
    log.info("[Auth] 사용자 로그아웃 - 아이디: {}", username);
  }
}
