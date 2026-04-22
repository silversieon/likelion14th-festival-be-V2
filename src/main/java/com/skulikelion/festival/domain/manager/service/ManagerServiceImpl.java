/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.manager.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.domain.booth.repository.BoothRepository;
import com.skulikelion.festival.domain.manager.dto.request.CreateManagerRequest;
import com.skulikelion.festival.domain.manager.dto.request.UpdateManagerPasswordRequest;
import com.skulikelion.festival.domain.manager.dto.request.UpdateManagerUsernameRequest;
import com.skulikelion.festival.domain.manager.dto.response.ManagerResponse;
import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.domain.manager.exception.ManagerErrorCode;
import com.skulikelion.festival.domain.manager.mapper.ManagerMapper;
import com.skulikelion.festival.domain.manager.repository.ManagerRepository;
import com.skulikelion.festival.global.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerServiceImpl implements ManagerService {

  private final PasswordEncoder passwordEncoder;
  private final BoothRepository boothRepository;
  private final ManagerRepository managerRepository;
  private final ManagerMapper managerMapper;

  @Override
  @Transactional
  public ManagerResponse createManager(CreateManagerRequest request) {
    if (managerRepository.findByUsername(request.getUsername()).isPresent()) {
      log.info("[ManagerService] 이미 존재하는 관리자 아이디 - {}", request.getUsername());
      throw new CustomException(ManagerErrorCode.MANAGER_ALREADY_EXIST);
    }

    Booth booth = null;
    if (request.getRole() == Role.BOOTH_MANAGER) {
      if (request.getBoothId() == null) {
        throw new CustomException(ManagerErrorCode.BOOTH_NOT_EXIST);
      }
      booth =
          boothRepository
              .findById(request.getBoothId())
              .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));
    }

    String encodedPassword = passwordEncoder.encode(request.getPassword());
    Manager manager =
        Manager.builder()
            .username(request.getUsername())
            .password(encodedPassword)
            .role(request.getRole())
            .booth(booth)
            .build();

    Manager savedManager = managerRepository.save(manager);
    return managerMapper.toManagerResponse(savedManager);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ManagerResponse> getManagers(Role role) {
    List<ManagerResponse> managerResponses = managerRepository.findManagersWithBoothByRole(role);
    log.info("[ManagerService] 관리자 전체 조회 발생");
    return managerResponses;
  }

  @Override
  @Transactional(readOnly = true)
  public ManagerResponse getManager(Long managerId) {
    ManagerResponse managerResponse =
        managerRepository
            .findManagerWithBoothById(managerId)
            .orElseThrow(() -> new CustomException(ManagerErrorCode.MANAGER_NOT_FOUND));
    log.info("[ManagerService] 관리자 단건 조회 발생 - 관리자 아이디: {}", managerResponse.getUsername());
    return managerResponse;
  }

  @Override
  @Transactional
  public ManagerResponse updateManagerUsername(
      Long managerId, UpdateManagerUsernameRequest request) {
    Manager manager =
        managerRepository
            .findById(managerId)
            .orElseThrow(() -> new CustomException(ManagerErrorCode.MANAGER_NOT_FOUND));
    manager.updateUsername(request.getUsername());
    log.info("[ManagerService] 관리자 아이디 변경 발생 - 변경된 관리자 아이디: {}", manager.getUsername());
    return managerMapper.toManagerResponse(manager);
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
    log.info("[ManagerService] 관리자 비밀번호 변경 발생 - 관리자 아이디: {}", manager.getUsername());
    return managerMapper.toManagerResponse(manager);
  }

  @Override
  @Transactional
  public Void deleteManager(Long managerId) {
    if (managerRepository.findById(managerId).isEmpty()) {
      throw new CustomException(ManagerErrorCode.MANAGER_NOT_FOUND);
    }
    managerRepository.deleteById(managerId);
    log.info("[ManagerService] 관리자 삭제 발생 - 관리자 식별자: {}", managerId);
    return null;
  }

  @Override
  @Transactional(readOnly = true)
  public ManagerResponse getMyInfo(String username) {
    Manager manager =
        managerRepository
            .findByUsername(username)
            .orElseThrow(() -> new CustomException(ManagerErrorCode.MANAGER_NOT_FOUND));
    log.info("[ManagerService] 관리자 본인 정보 조회 발생 - 관리자 아이디: {}", manager.getUsername());
    return managerMapper.toManagerResponse(manager);
  }
}
