/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.service;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.booth.dto.request.BoothLoginRequest;
import com.skulikelion.festival.domain.booth.dto.response.BoothResponse;
import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.entity.OpeningHours;
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.domain.booth.mapper.BoothMapper;
import com.skulikelion.festival.domain.booth.repository.BoothRepository;
import com.skulikelion.festival.global.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoothService {

  private final BoothRepository boothRepository;
  private final BoothMapper boothMapper;

  /**
   * 모든 부스 정보를 조회합니다.
   *
   * @return 등록된 모든 부스의 응답 DTO 목록
   */
  @Transactional(readOnly = true)
  public List<BoothResponse> getAllBooths() {
    log.info("[부스 전체 조회 GET: /api/booths] 전체 부스 목록 조회 요청");
    List<Booth> booths = boothRepository.findAll();
    return booths.stream().map(boothMapper::toBoothResponse).toList();
  }

  /**
   * 부스 이름으로 해당 부스의 운영 시간을 조회합니다.
   *
   * @param id 조회할 부스의 식별자
   * @return 해당 부스의 운영 시간
   * @throws CustomException 해당 이름의 부스를 찾을 수 없는 경우
   */
  @Transactional(readOnly = true)
  public OpeningHours getBoothOpeningHoursById(Long id) {
    Booth booth =
        boothRepository
            .findById(id)
            .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));

    log.info("[부스 운영 시간 조회 GET: /api/booths/{}] {} 부스 운영 시간 조회 요청", id, booth.getName());
    return booth.getOpeningHours();
  }

  /**
   * 부스 로그인 메서드
   *
   * @param boothLoginRequest 부스 로그인 요청 정보 (부스 이름과 비밀번호 포함)
   * @return 로그인 성공 시 부스 정보 응답 DTO
   * @throws CustomException 부스를 찾을 수 없거나 비밀번호가 일치하지 않는 경우
   */
  public BoothResponse login(BoothLoginRequest boothLoginRequest, HttpServletRequest request) {
    // 부스 존재 확인
    Booth booth =
        boothRepository
            .findByName(boothLoginRequest.getName())
            .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));

    // 비밀번호 확인
    if (!booth.getPassword().equals(boothLoginRequest.getPassword())) {
      throw new CustomException(BoothErrorCode.INVALID_PASSWORD);
    }

    // 인증 객체 생성
    UsernamePasswordAuthenticationToken authToken =
        new UsernamePasswordAuthenticationToken(booth.getName(), null, List.of());
    SecurityContextHolder.getContext().setAuthentication(authToken);

    // 세션을 생성하고, Spring Security가 자동으로 세션 쿠키(JSESSIONID)를 클라이언트에 전송
    request.getSession(true); // `true`는 세션을 생성하거나 기존 세션을 반환

    log.info("[로그인 성공 POST: /api/booths/login] 부스 이름: {}", booth.getName());
    return boothMapper.toBoothResponse(booth);
  }

  /**
   * 부스의 대기 팀 수를 조회합니다.
   *
   * @param id 부스 식별자
   * @return 대기 팀 수
   * @throws CustomException 부스를 찾을 수 없는 경우
   */
  @Transactional(readOnly = true)
  public int getWaitingTeamCount(Long id) {
    Booth booth =
        boothRepository
            .findById(id)
            .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));

    log.info(
        "[부스 대기 팀 수 조회 GET: /api/dev/{}] 현재 대기 팀 수: {}", booth.getName(), booth.getWaitingTeam());
    return booth.getWaitingTeam();
  }
}
