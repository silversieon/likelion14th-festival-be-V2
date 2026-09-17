/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.skulikelion.festival.domain.auth.dto.request.LoginRequest;
import com.skulikelion.festival.domain.auth.dto.request.SignUpRequest;
import com.skulikelion.festival.domain.auth.exception.AuthErrorCode;
import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.domain.manager.repository.ManagerRepository;
import com.skulikelion.festival.domain.university.entity.Department;
import com.skulikelion.festival.domain.university.entity.University;
import com.skulikelion.festival.domain.university.enums.Region;
import com.skulikelion.festival.domain.university.exception.UniversityErrorCode;
import com.skulikelion.festival.domain.university.repository.DepartmentRepository;
import com.skulikelion.festival.domain.university.repository.UniversityRepository;
import com.skulikelion.festival.global.config.property.AuthProperties;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.infra.redis.RefreshTokenRepository;
import com.skulikelion.festival.global.security.CustomUserDetails;
import com.skulikelion.festival.global.security.jwt.JwtProvider;
import com.skulikelion.festival.global.security.jwt.internal.GeneratedRefreshTokenPayload;

/**
 * {@link AuthServiceImpl} 테스트입니다. (LLD-0001 13.1 E그룹)
 *
 * @since 2026.09.14
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl은")
class AuthServiceImplTest {

  private static final String ADMIN_KEY = "correct-admin-key";
  private static final Long UNIVERSITY_ID = 1L;
  private static final Long DEPARTMENT_ID = 12L;

  @Mock private JwtProvider jwtProvider;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private UserDetailsService userDetailsService;
  @Mock private ManagerRepository managerRepository;
  @Mock private UniversityRepository universityRepository;
  @Mock private DepartmentRepository departmentRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @Captor private ArgumentCaptor<Manager> managerCaptor;
  @Captor private ArgumentCaptor<UsernamePasswordAuthenticationToken> authTokenCaptor;

  private AuthServiceImpl authService;

  private static University university() {
    return University.builder().id(UNIVERSITY_ID).name("서경대학교").region(Region.SEOUL).build();
  }

  private static Department department() {
    return Department.builder().id(DEPARTMENT_ID).university(university()).name("소프트웨어학과").build();
  }

  private static Manager manager() {
    return Manager.builder()
        .id(1024L)
        .department(department())
        .password("encoded")
        .role(Role.BOOTH_MANAGER)
        .build();
  }

  private static SignUpRequest signUpRequest(String adminKey) {
    return SignUpRequest.builder()
        .universityId(UNIVERSITY_ID)
        .departmentId(DEPARTMENT_ID)
        .password("lion1234!")
        .role(Role.BOOTH_MANAGER)
        .adminKey(adminKey)
        .build();
  }

  private static LoginRequest loginRequest() {
    return LoginRequest.builder()
        .universityId(UNIVERSITY_ID)
        .departmentId(DEPARTMENT_ID)
        .password("lion1234!")
        .build();
  }

  private void setUpService() {
    authService =
        new AuthServiceImpl(
            jwtProvider,
            authenticationManager,
            refreshTokenRepository,
            userDetailsService,
            managerRepository,
            universityRepository,
            departmentRepository,
            passwordEncoder,
            new AuthProperties(ADMIN_KEY));
  }

  @Nested
  @DisplayName("signUp은")
  class SignUp {

    @Test
    @DisplayName("어드민 키가 다르면 INCORRECT_ADMIN_KEY를 던지고 저장하지 않는다")
    void throwsIncorrectAdminKey_andDoesNotSave() {
      setUpService();

      assertThatThrownBy(() -> authService.signUp(signUpRequest("wrong-key")))
          .isInstanceOf(CustomException.class)
          .extracting(e -> ((CustomException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.INCORRECT_ADMIN_KEY);

      verify(managerRepository, never()).save(any());
    }

    @Test
    @DisplayName("대학이 없으면 UNIVERSITY_NOT_FOUND를 던진다")
    void throwsUniversityNotFound() {
      setUpService();
      given(universityRepository.existsById(UNIVERSITY_ID)).willReturn(false);

      assertThatThrownBy(() -> authService.signUp(signUpRequest(ADMIN_KEY)))
          .isInstanceOf(CustomException.class)
          .extracting(e -> ((CustomException) e).getErrorCode())
          .isEqualTo(UniversityErrorCode.UNIVERSITY_NOT_FOUND);
    }

    @Test
    @DisplayName("학과가 없으면 DEPARTMENT_NOT_FOUND를 던진다")
    void throwsDepartmentNotFound() {
      setUpService();
      given(universityRepository.existsById(UNIVERSITY_ID)).willReturn(true);
      given(departmentRepository.findById(DEPARTMENT_ID)).willReturn(Optional.empty());

      assertThatThrownBy(() -> authService.signUp(signUpRequest(ADMIN_KEY)))
          .isInstanceOf(CustomException.class)
          .extracting(e -> ((CustomException) e).getErrorCode())
          .isEqualTo(UniversityErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("학과가 그 대학 소속이 아니면 DEPARTMENT_NOT_IN_UNIVERSITY를 던진다")
    void throwsDepartmentNotInUniversity() {
      setUpService();
      University otherUniversity =
          University.builder().id(2L).name("다른대학교").region(Region.GYEONGGI).build();
      Department otherDepartment =
          Department.builder()
              .id(DEPARTMENT_ID)
              .university(otherUniversity)
              .name("소프트웨어학과")
              .build();
      given(universityRepository.existsById(UNIVERSITY_ID)).willReturn(true);
      given(departmentRepository.findById(DEPARTMENT_ID)).willReturn(Optional.of(otherDepartment));

      assertThatThrownBy(() -> authService.signUp(signUpRequest(ADMIN_KEY)))
          .isInstanceOf(CustomException.class)
          .extracting(e -> ((CustomException) e).getErrorCode())
          .isEqualTo(UniversityErrorCode.DEPARTMENT_NOT_IN_UNIVERSITY);
    }

    @Test
    @DisplayName("그 학과에 이미 관리자가 있으면 ALREADY_EXIST_MANAGER를 던진다")
    void throwsAlreadyExistManager() {
      setUpService();
      given(universityRepository.existsById(UNIVERSITY_ID)).willReturn(true);
      given(departmentRepository.findById(DEPARTMENT_ID)).willReturn(Optional.of(department()));
      given(managerRepository.existsByDepartmentId(DEPARTMENT_ID)).willReturn(true);

      assertThatThrownBy(() -> authService.signUp(signUpRequest(ADMIN_KEY)))
          .isInstanceOf(CustomException.class)
          .extracting(e -> ((CustomException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.ALREADY_EXIST_MANAGER);
    }

    @Test
    @DisplayName("정상 요청이면 비밀번호를 인코딩해 저장한다")
    void savesManagerWithEncodedPassword() {
      setUpService();
      given(universityRepository.existsById(UNIVERSITY_ID)).willReturn(true);
      given(departmentRepository.findById(DEPARTMENT_ID)).willReturn(Optional.of(department()));
      given(managerRepository.existsByDepartmentId(DEPARTMENT_ID)).willReturn(false);
      given(passwordEncoder.encode("lion1234!")).willReturn("encoded-password");
      given(managerRepository.save(any(Manager.class))).willReturn(manager());

      authService.signUp(signUpRequest(ADMIN_KEY));

      verify(managerRepository).save(managerCaptor.capture());
      Manager saved = managerCaptor.getValue();
      assertThat(saved.getPassword()).isEqualTo("encoded-password");
      assertThat(saved.getDepartment().getId()).isEqualTo(DEPARTMENT_ID);
    }
  }

  @Nested
  @DisplayName("login은")
  class Login {

    @Test
    @DisplayName("해당 학과에 관리자가 없으면 LOGIN_FAIL을 던진다")
    void throwsLoginFail_whenManagerNotFound() {
      setUpService();
      given(universityRepository.existsById(UNIVERSITY_ID)).willReturn(true);
      given(departmentRepository.findById(DEPARTMENT_ID)).willReturn(Optional.of(department()));
      given(managerRepository.findByDepartmentId(DEPARTMENT_ID)).willReturn(Optional.empty());

      assertThatThrownBy(() -> authService.login(loginRequest()))
          .isInstanceOf(CustomException.class)
          .extracting(e -> ((CustomException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.LOGIN_FAIL);
    }

    @Test
    @DisplayName("인증 매니저에 관리자 식별자를 username으로 넘기고 토큰을 발급한다")
    void authenticatesWithManagerIdAndIssuesTokens() {
      setUpService();
      Manager manager = manager();
      Authentication authentication =
          new UsernamePasswordAuthenticationToken(
              new CustomUserDetails(manager),
              null,
              new CustomUserDetails(manager).getAuthorities());

      given(universityRepository.existsById(UNIVERSITY_ID)).willReturn(true);
      given(departmentRepository.findById(DEPARTMENT_ID)).willReturn(Optional.of(department()));
      given(managerRepository.findByDepartmentId(DEPARTMENT_ID)).willReturn(Optional.of(manager));
      given(authenticationManager.authenticate(any())).willReturn(authentication);
      given(jwtProvider.generateAccessToken(any(), any())).willReturn("access-token");
      given(jwtProvider.generateRefreshToken(any()))
          .willReturn(new GeneratedRefreshTokenPayload("refresh-token", "jti"));

      var tokenResponse = authService.login(loginRequest());

      verify(authenticationManager).authenticate(authTokenCaptor.capture());
      assertThat(authTokenCaptor.getValue().getPrincipal()).isEqualTo("1024");
      assertThat(tokenResponse.getAccessToken()).isEqualTo("access-token");
      assertThat(tokenResponse.getRefreshToken()).isEqualTo("refresh-token");
      verify(refreshTokenRepository).saveRefreshToken("refresh-token", "jti");
    }
  }
}
