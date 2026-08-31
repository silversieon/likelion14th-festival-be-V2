/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.manager.service;

import java.util.List;

import com.skulikelion.festival.domain.manager.dto.request.UpdateManagerPasswordRequest;
import com.skulikelion.festival.domain.manager.dto.response.ManagerResponse;
import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.entity.enums.Role;

/**
 * 멋쟁이사자처럼 서경대학교 축제 페이지 관리자 관련 Service interface 입니다.
 *
 * @see com.skulikelion.festival.domain.manager.controller.ManagerController
 * @since 2026.04.21
 * @author Keum Si Eon
 */
public interface ManagerService {

  /**
   * [ 관리자 전체 조회 메서드 ]
   *
   * @param role 조회할 관리자의 역할군
   * @return 관리자 정보 리스트
   */
  List<ManagerResponse> getManagers(Role role);

  /**
   * [ 관리자 단건 조회 메서드 ]
   *
   * @param managerId 조회할 관리자의 식별자
   * @return 관리자 정보
   */
  ManagerResponse getManager(Long managerId);

  /**
   * [ 관리자 비밀번호 수정 메서드 ]
   *
   * @param managerId 비밀번호 정보를 수정할 관리자의 식별자
   * @return
   */
  ManagerResponse updateManagerPassword(Long managerId, UpdateManagerPasswordRequest request);

  /**
   * [ 관리자 삭제 메서드 ]
   *
   * @param managerId 삭제할 관리자의 식별자
   * @return
   */
  void deleteManager(Long managerId);

  /**
   * [ 관리자 본인 정보 조회 메서드 ]
   *
   * @param departmentName 사용자 아이디
   * @return
   */
  ManagerResponse getMyInfo(String departmentName);

  /**
   * [ 관리자 조회 메서드 ] 학과명으로 관리자 조회
   *
   * @param departmentName 학과명
   * @return 관리자 엔티티
   */
  Manager getRequiredManager(String departmentName);
}
