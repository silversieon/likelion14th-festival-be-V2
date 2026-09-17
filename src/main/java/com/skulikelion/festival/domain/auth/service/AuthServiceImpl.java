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
import com.skulikelion.festival.domain.university.entity.Department;
import com.skulikelion.festival.domain.university.exception.UniversityErrorCode;
import com.skulikelion.festival.domain.university.repository.DepartmentRepository;
import com.skulikelion.festival.domain.university.repository.UniversityRepository;
import com.skulikelion.festival.global.config.property.AuthProperties;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.infra.redis.RefreshTokenRepository;
import com.skulikelion.festival.global.security.AuthPrincipal;
import com.skulikelion.festival.global.security.CustomUserDetails;
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
  private final UniversityRepository universityRepository;
  private final DepartmentRepository departmentRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthProperties authProperties;

  @Override
  @Transactional
  public void signUp(SignUpRequest request) {
    if (!authProperties.getAdminKey().equals(request.getAdminKey())) {
      log.error("[Auth] 잘못된 어드민 키 입력");
      throw new CustomException(AuthErrorCode.INCORRECT_ADMIN_KEY);
    }

    Department department = getDepartmentOf(request.getUniversityId(), request.getDepartmentId());
    if (managerRepository.existsByDepartmentId(department.getId())) {
      log.warn("[Auth] 이미 관리자가 존재하는 학과로 회원가입 시도 - 학과 식별자: {}", department.getId());
      throw new CustomException(AuthErrorCode.ALREADY_EXIST_MANAGER);
    }

    String encodedPassword = passwordEncoder.encode(request.getPassword());
    Manager manager =
        Manager.builder()
            .department(department)
            .role(request.getRole())
            .password(encodedPassword)
            .build();
    Manager savedManager = managerRepository.save(manager);

    log.info(
        "[Auth] 신규 사용자 회원가입 - 관리자 식별자: {}, 학과: {}", savedManager.getId(), department.getName());
  }

  @Override
  @Transactional(readOnly = true)
  @TimeTrace(
      methodName = "로그인",
      env = {"local", "dev"})
  public TokenResponse login(LoginRequest request) {
    Department department = getDepartmentOf(request.getUniversityId(), request.getDepartmentId());

    try {
      Manager manager =
          managerRepository
              .findByDepartmentId(department.getId())
              .orElseThrow(
                  () -> {
                    // 계정 존재 여부가 유추되지 않도록 비밀번호 불일치와 같은 예외로 통일한다 (policy 13.2).
                    log.info("[AuthService] 로그인 실패 - 관리자가 없는 학과 식별자: {}", department.getId());
                    return new CustomException(AuthErrorCode.LOGIN_FAIL);
                  });

      // UserDetailsService가 단일 문자열만 받으므로, (대학, 학과)로 찾은 매니저를 식별자로 바꿔 넘긴다 (ADR-0001 옵션 2).
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  String.valueOf(manager.getId()), request.getPassword()));

      TokenResponse tokenResponse = issueTokens(authentication);

      log.info(
          "[AuthService] 사용자 로그인 성공 - 관리자 식별자: {}, 학과: {}", manager.getId(), department.getName());
      return tokenResponse;
    } catch (BadCredentialsException | UsernameNotFoundException e) {
      log.info("[AuthService] 로그인 실패 - 학과 식별자: {}", request.getDepartmentId());
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

    Long managerId = jwtProvider.getManagerIdFromToken(refreshToken);
    UserDetails userDetails = userDetailsService.loadUserByUsername(String.valueOf(managerId));
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

    return issueTokens(authentication);
  }

  @Override
  public void logout(String refreshToken) {
    Long managerId = jwtProvider.getManagerIdFromToken(refreshToken);
    String jti = jwtProvider.getJtiFromToken(refreshToken);
    refreshTokenRepository.deleteRefreshToken(jti);
    log.info("[AuthService] 사용자 로그아웃 - 관리자 식별자: {}", managerId);
  }

  /**
   * [ 인증 결과로 Access/Refresh 토큰을 발급하고 Refresh를 저장하는 메서드 ]
   *
   * @param authentication 인증 성공 결과 (principal이 {@link CustomUserDetails})
   * @return 발급된 토큰 쌍
   */
  private TokenResponse issueTokens(Authentication authentication) {
    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    AuthPrincipal principal = userDetails.toPrincipal();

    String accessToken = jwtProvider.generateAccessToken(principal, userDetails.getAuthorities());
    GeneratedRefreshTokenPayload generatedRefreshTokenPayload =
        jwtProvider.generateRefreshToken(principal);
    refreshTokenRepository.saveRefreshToken(
        generatedRefreshTokenPayload.token(), generatedRefreshTokenPayload.jti());

    return TokenResponse.builder()
        .accessToken(accessToken)
        .refreshToken(generatedRefreshTokenPayload.token())
        .build();
  }

  /**
   * [ 대학·학과를 조회하고 소속 관계를 검증하는 메서드 ]
   *
   * <p>소속 검증은 서비스가 아니라 {@link Department} 엔티티가 강제한다 (policy 9.2).
   *
   * @param universityId 대학 식별자
   * @param departmentId 학과 식별자
   * @return 검증된 학과
   */
  private Department getDepartmentOf(Long universityId, Long departmentId) {
    if (!universityRepository.existsById(universityId)) {
      log.warn("[AuthService] 존재하지 않는 대학 식별자 입력 - 대학 식별자: {}", universityId);
      throw new CustomException(UniversityErrorCode.UNIVERSITY_NOT_FOUND);
    }
    Department department =
        departmentRepository
            .findById(departmentId)
            .orElseThrow(
                () -> {
                  log.warn("[AuthService] 존재하지 않는 학과 식별자 입력 - 학과 식별자: {}", departmentId);
                  return new CustomException(UniversityErrorCode.DEPARTMENT_NOT_FOUND);
                });
    department.validateBelongsTo(universityId);
    return department;
  }
}
