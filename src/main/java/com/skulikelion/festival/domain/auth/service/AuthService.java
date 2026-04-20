/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.auth.service;

import com.skulikelion.festival.domain.auth.dto.request.LoginRequest;
import com.skulikelion.festival.domain.auth.dto.request.SignUpRequest;
import com.skulikelion.festival.domain.auth.dto.response.TokenResponse;

/**
 * 멋쟁이사자처럼 서경대학교 축제 페이지 인증 관련 Service interface 입니다.
 *
 * @see com.skulikelion.festival.domain.auth.controller.AuthController
 * @since 2026.04.20
 * @author Keum Si Eon
 */
public interface AuthService {

  /**
   * [ 사용자 회원가입 메서드 ]
   *
   * @param request 회원가입 요청을 위한 사용자 정보를 담은 요청 객체 (어드민 키 포함)
   */
  void signUp(SignUpRequest request);

  /**
   * [ 사용자 로그인 메서드 ]
   *
   * @param request 로그인 요청을 위한 아이디, 비밀번호를 담은 요청 객체
   * @return accessToken, refreshToken을 담은 TokenResponse 객체
   */
  TokenResponse login(LoginRequest request);

  /**
   * [ 사용자 토큰 리프레시 메서드 ]
   *
   * @param refreshToken 토큰 재발급 요청에 사용될 리프레시 토큰
   * @return 재발급된 accessToken, refreshToken을 담은 TokenResponse 객체
   */
  TokenResponse refresh(String refreshToken);

  /**
   * [ 로그아웃 메서드 ]
   *
   * @param refreshToken 삭제 및 블랙리스트 처리 할 리프레시 토큰
   */
  void logout(String refreshToken);
}
