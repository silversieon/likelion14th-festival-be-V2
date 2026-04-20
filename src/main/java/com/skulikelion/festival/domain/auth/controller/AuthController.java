/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skulikelion.festival.domain.auth.dto.request.LoginRequest;
import com.skulikelion.festival.domain.auth.dto.request.SignUpRequest;
import com.skulikelion.festival.domain.auth.dto.response.TokenResponse;
import com.skulikelion.festival.domain.auth.exception.AuthErrorCode;
import com.skulikelion.festival.domain.auth.service.AuthService;
import com.skulikelion.festival.global.common.BaseResponse;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.security.jwt.JwtCookieWriter;
import com.skulikelion.festival.global.security.jwt.JwtProvider;
import com.skulikelion.festival.global.security.jwt.TokenType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "사용자 인증 및 검증 관련 기능을 제공하는 API")
public class AuthController {

  private final AuthService authService;
  private final JwtCookieWriter jwtCookieWriter;
  private final JwtProvider jwtProvider;

  @Operation(
      summary = "[ 사용자 | 토큰 X | 회원가입 ]",
      description =
          """
            **Parameters**  \n
            username: 사용자 아이디 \n
            password: 사용자 비밀번호 \n
            adminKey: 관리자 키 \n

            **Returns**  \n
            회원가입 성공 여부
            """)
  @PostMapping("/register")
  public ResponseEntity<BaseResponse<Void>> register(@Valid @RequestBody SignUpRequest request) {
    authService.signUp(request);
    return ResponseEntity.status(201).body(BaseResponse.success(201, "회원가입에 성공했습니다.", null));
  }

  @Operation(
      summary = "[ 사용자 | 토큰 X | 로그인 ]",
      description =
          """
                    **Parameters**  \n
                    username: 사용자 아이디  \n
                    password: 사용자 비밀번호 \n

                    **Returns (쿠키에 전달)**  \n
                    ACCESS_TOKEN: JWT 액세스 토큰 \n
                    REFRESH_TOKEN: JWT 리프레시 토큰 \n
                    """)
  @PostMapping("/login")
  public ResponseEntity<BaseResponse<Void>> login(@Valid @RequestBody LoginRequest request) {
    TokenResponse tokenResponse = authService.login(request);
    HttpHeaders tokenHeaders = new HttpHeaders();
    tokenHeaders.add(
        HttpHeaders.SET_COOKIE,
        jwtCookieWriter.addAccessTokenToCookie(tokenResponse.getAccessToken()).toString());
    tokenHeaders.add(
        HttpHeaders.SET_COOKIE,
        jwtCookieWriter.addRefreshTokenToCookie(tokenResponse.getRefreshToken()).toString());
    return ResponseEntity.status(200)
        .headers(tokenHeaders)
        .body(BaseResponse.success(200, "로그인에 성공했습니다.", null));
  }

  @Operation(
      summary = "[ 사용자 | 토큰 O | 토큰 재발급 ]",
      description =
          """
                    **Returns (쿠키에 전달)**  \n
                    ACCESS_TOKEN: JWT 액세스 토큰 \n
                    REFRESH_TOKEN: JWT 리프레시 토큰 \n
                    """)
  @PostMapping("/refresh")
  public ResponseEntity<BaseResponse<Void>> refresh(HttpServletRequest request) {
    String refreshToken = jwtProvider.extractRefreshToken(request);
    validateRefreshToken(refreshToken);
    TokenResponse tokenResponse = authService.refresh(refreshToken);
    HttpHeaders tokenHeaders = new HttpHeaders();
    tokenHeaders.add(
        HttpHeaders.SET_COOKIE,
        jwtCookieWriter.addAccessTokenToCookie(tokenResponse.getAccessToken()).toString());
    tokenHeaders.add(
        HttpHeaders.SET_COOKIE,
        jwtCookieWriter.addRefreshTokenToCookie(tokenResponse.getRefreshToken()).toString());
    return ResponseEntity.status(200)
        .headers(tokenHeaders)
        .body(BaseResponse.success(200, "토큰 재발급에 성공했습니다.", null));
  }

  @Operation(
      summary = "[ 사용자 | 토큰 O | 로그아웃 ]",
      description =
          """
                    **Returns (쿠키에 전달 [개발에서는 응답값 활용])**  \n
                    ACCESS_TOKEN: 0초 후 만료되는 ACCESS_TOKEN \n
                    REFRESH_TOKEN: 0초 후 만료되는 REFRESH_TOKEN \n
                    """)
  @PostMapping("/logout")
  public ResponseEntity<BaseResponse<Void>> logout(HttpServletRequest request) {
    String refreshToken = jwtProvider.extractRefreshToken(request);
    validateRefreshToken(refreshToken);
    authService.logout(refreshToken);
    HttpHeaders tokenHeaders = new HttpHeaders();
    tokenHeaders.add(
        HttpHeaders.SET_COOKIE,
        jwtCookieWriter.removeTokenFromCookie(TokenType.ACCESS_TOKEN).toString());
    tokenHeaders.add(
        HttpHeaders.SET_COOKIE,
        jwtCookieWriter.removeTokenFromCookie(TokenType.REFRESH_TOKEN).toString());

    return ResponseEntity.status(200)
        .headers(tokenHeaders)
        .body(BaseResponse.success(200, "로그아웃에 성공하였습니다.", null));
  }

  private void validateRefreshToken(String refreshToken) {
    if (!jwtProvider.validateToken(refreshToken, TokenType.REFRESH_TOKEN)) {
      log.info("[AuthController] 유효하지 않은 리프레시 토큰을 통한 리프레시 요청");
      throw new CustomException(AuthErrorCode.UNAUTHORIZED_TOKEN);
    }
  }
}
