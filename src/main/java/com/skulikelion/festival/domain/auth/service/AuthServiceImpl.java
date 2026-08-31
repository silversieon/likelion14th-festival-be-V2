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
import com.skulikelion.festival.global.config.property.AuthProperties;
import com.skulikelion.festival.global.enums.Department;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.infra.redis.RefreshTokenRepository;
import com.skulikelion.festival.global.security.jwt.JwtProvider;
import com.skulikelion.festival.global.security.jwt.TokenType;
import com.skulikelion.festival.global.security.jwt.internal.GeneratedRefreshTokenPayload;
import com.skulikelion.festival.global.util.timetrace.annotation.TimeTrace;

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
    Department department = validateDepartmentName(request.getDepartmentName());
    if (managerRepository.findByDepartment(department).isPresent()) {
      log.warn("[Auth] 존재하는 아이디 입력 - 학과명: {}", department.getDescription());
      throw new CustomException(AuthErrorCode.ALREADY_EXIST_DEPARTMENT);
    }

    String encodedPassword = passwordEncoder.encode(request.getPassword());
    Manager manager =
        Manager.builder()
            .department(department)
            .role(request.getRole())
            .password(encodedPassword)
            .build();
    Manager savedManager = managerRepository.save(manager);

    log.info("[Auth] 신규 사용자 회원가입 - 학과명: {}", savedManager.getDepartment().getDescription());
  }

  @Override
  @Transactional(readOnly = true)
  @TimeTrace(
      methodName = "로그인",
      env = {"local", "dev"})
  public TokenResponse login(LoginRequest request) {
    try {
      Department department = validateDepartmentName(request.getDepartmentName());
      UsernamePasswordAuthenticationToken authenticationToken =
          new UsernamePasswordAuthenticationToken(department.name(), request.getPassword());
      Authentication authentication = authenticationManager.authenticate(authenticationToken);

      String accessToken = jwtProvider.generateAccessToken(authentication);

      GeneratedRefreshTokenPayload generatedRefreshTokenPayload =
          jwtProvider.generateRefreshToken(authentication);
      String refreshToken = generatedRefreshTokenPayload.token();
      String jti = generatedRefreshTokenPayload.jti();
      refreshTokenRepository.saveRefreshToken(refreshToken, jti);

      TokenResponse tokenResponse =
          TokenResponse.builder().accessToken(accessToken).refreshToken(refreshToken).build();

      log.info("[AuthService] 사용자 로그인 성공 - 학과명: {}", department.getDescription());
      return tokenResponse;
    } catch (BadCredentialsException | UsernameNotFoundException e) {
      log.info("[AuthService] 로그인 실패 - 입력한 학과명: {}", request.getDepartmentName());
      throw new CustomException(AuthErrorCode.LOGIN_FAIL);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public TokenResponse refresh(String refreshToken) {
    jwtProvider.validateToken(refreshToken, TokenType.REFRESH_TOKEN);

    String jti = jwtProvider.getJtiFromToken(refreshToken);
    refreshTokenRepository.validateStoredRefreshToken(refreshToken, jti);

    refreshTokenRepository.deleteRefreshToken(jti);

    String departmentName = jwtProvider.getDepartmentFromToken(refreshToken);
    UserDetails userDetails = userDetailsService.loadUserByUsername(departmentName);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(departmentName, null, userDetails.getAuthorities());

    String newAccessToken = jwtProvider.generateAccessToken(authentication);
    GeneratedRefreshTokenPayload generatedRefreshTokenPayload =
        jwtProvider.generateRefreshToken(authentication);
    String newRefreshToken = generatedRefreshTokenPayload.token();

    refreshTokenRepository.saveRefreshToken(newRefreshToken, generatedRefreshTokenPayload.jti());

    return TokenResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(newRefreshToken)
        .build();
  }

  @Override
  public void logout(String refreshToken) {
    Department department = Department.valueOf(jwtProvider.getDepartmentFromToken(refreshToken));
    String jti = jwtProvider.getJtiFromToken(refreshToken);
    refreshTokenRepository.deleteRefreshToken(jti);
    log.info("[AuthService] 사용자 로그아웃 - 학과명: {}", department.getDescription());
  }

  private Department validateDepartmentName(String departmentName) {
    try {
      return Department.valueOf(departmentName);
    } catch (IllegalArgumentException e) {
      log.warn("[AuthService] 입력한 학과명 : {}", departmentName);
      throw new CustomException(AuthErrorCode.INVALID_DEPARTMENT);
    }
  }
}
