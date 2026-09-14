/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.manager.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.manager.dto.request.UpdateManagerPasswordRequest;
import com.skulikelion.festival.domain.manager.dto.response.ManagerResponse;
import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.domain.manager.exception.ManagerErrorCode;
import com.skulikelion.festival.domain.manager.mapper.ManagerMapper;
import com.skulikelion.festival.domain.manager.repository.ManagerRepository;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.security.AuthPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerServiceImpl implements ManagerService {

  private final PasswordEncoder passwordEncoder;
  private final ManagerRepository managerRepository;
  private final ManagerMapper managerMapper;

  @Override
  @Transactional(readOnly = true)
  public List<ManagerResponse> getManagers(Role role) {
    List<ManagerResponse> managerResponses = managerRepository.findManagersByRole(role);
    log.info("[ManagerService] 관리자 전체 조회 발생");
    return managerResponses;
  }

  @Override
  @Transactional(readOnly = true)
  public ManagerResponse getManager(Long managerId) {
    ManagerResponse managerResponse =
        managerRepository
            .findManagerById(managerId)
            .orElseThrow(() -> new CustomException(ManagerErrorCode.MANAGER_NOT_FOUND));
    log.info("[ManagerService] 관리자 단건 조회 발생 - 관리자 학과명: {}", managerResponse.getDepartmentName());
    return managerResponse;
  }

  @Override
  @Transactional
  public ManagerResponse updateManagerPassword(
      Long managerId, UpdateManagerPasswordRequest request) {
    Manager manager =
        managerRepository
            .findById(managerId)
            .orElseThrow(() -> new CustomException(ManagerErrorCode.MANAGER_NOT_FOUND));
    String encodedPassword = passwordEncoder.encode(request.getPassword());
    manager.updatePassword(encodedPassword);
    log.info("[ManagerService] 관리자 비밀번호 변경 발생 - 관리자 식별자: {}", manager.getId());
    return managerMapper.toManagerResponse(manager);
  }

  @Override
  @Transactional
  public void deleteManager(Long managerId) {
    if (managerRepository.findById(managerId).isEmpty()) {
      throw new CustomException(ManagerErrorCode.MANAGER_NOT_FOUND);
    }
    managerRepository.deleteById(managerId);
    log.info("[ManagerService] 관리자 삭제 발생 - 관리자 식별자: {}", managerId);
  }

  @Override
  @Transactional(readOnly = true)
  public ManagerResponse getMyInfo(AuthPrincipal principal) {
    Manager manager =
        managerRepository
            .findById(principal.managerId())
            .orElseThrow(() -> new CustomException(ManagerErrorCode.MANAGER_NOT_FOUND));
    log.info("[ManagerService] 관리자 본인 정보 조회 발생 - 관리자 식별자: {}", manager.getId());
    return managerMapper.toManagerResponse(manager);
  }

  @Override
  @Transactional(readOnly = true)
  public Manager getRequiredManager(Long managerId) {
    return managerRepository
        .findById(managerId)
        .orElseThrow(
            () -> {
              log.warn("[ManagerService] 매니저를 찾을 수 없습니다 - 관리자 식별자: {}", managerId);
              return new CustomException(ManagerErrorCode.MANAGER_NOT_FOUND);
            });
  }
}
