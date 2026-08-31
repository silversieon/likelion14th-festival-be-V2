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
import com.skulikelion.festival.global.enums.Department;
import com.skulikelion.festival.global.exception.CustomException;

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
    log.info(
        "[ManagerService] 관리자 단건 조회 발생 - 관리자 학과명: {}",
        managerResponse.getDepartment().getDescription());
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
    log.info(
        "[ManagerService] 관리자 비밀번호 변경 발생 - 관리자 학과명: {}", manager.getDepartment().getDescription());
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
  public ManagerResponse getMyInfo(String departmentName) {
    Department department = Department.valueOf(departmentName);
    Manager manager =
        managerRepository
            .findByDepartment(department)
            .orElseThrow(() -> new CustomException(ManagerErrorCode.MANAGER_NOT_FOUND));
    log.info(
        "[ManagerService] 관리자 본인 정보 조회 발생 - 관리자 학과명: {}", manager.getDepartment().getDescription());
    return managerMapper.toManagerResponse(manager);
  }

  @Override
  @Transactional(readOnly = true)
  public Manager getRequiredManager(String departmentName) {
    Department department = Department.valueOf(departmentName);
    return managerRepository
        .findByDepartment(department)
        .orElseThrow(
            () -> {
              log.warn("[OrderSseService] 해당 학과의 매니저를 찾을 수 없습니다 - 학과명: {}", departmentName);
              return new CustomException(ManagerErrorCode.MANAGER_NOT_FOUND);
            });
  }
}
