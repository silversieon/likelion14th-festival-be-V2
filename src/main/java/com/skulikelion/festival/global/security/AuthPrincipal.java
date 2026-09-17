/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.security;

import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.domain.university.entity.Department;

/**
 * 인증된 요청의 주체입니다. 컨트롤러는 {@code @AuthenticationPrincipal AuthPrincipal}로 받는다.
 *
 * <p>전국 확장 이전에는 principal이 {@code Department} enum 이름 문자열이었다. 학과명은 바뀔 수 있는 자연 키라, 이름이 바뀌면 발급된 토큰과
 * 소유권 판정이 함께 어긋났다. 그래서 대리 키({@code managerId})를 주체로 삼고, 자원 소유권 판정에 쓰는 {@code departmentId}를 토큰
 * 클레임으로 함께 싣는다. 덕분에 인가 때마다 {@code managers}를 다시 조회하지 않는다. (ADR-0001 옵션 2)
 *
 * @param managerId 관리자 식별자 — JWT subject
 * @param departmentId 소속 학과 식별자 — 자원 소유권 판정의 기준
 * @param universityId 소속 대학 식별자
 * @param role 관리자 역할
 * @since 2026.09.14
 */
public record AuthPrincipal(Long managerId, Long departmentId, Long universityId, Role role) {

  public static AuthPrincipal from(Manager manager) {
    Department department = manager.getDepartment();
    return new AuthPrincipal(
        manager.getId(), department.getId(), department.getUniversity().getId(), manager.getRole());
  }

  public boolean isAdmin() {
    return role == Role.ADMIN;
  }
}
