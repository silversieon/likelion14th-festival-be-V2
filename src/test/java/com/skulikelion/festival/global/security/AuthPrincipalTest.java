/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.domain.university.entity.Department;
import com.skulikelion.festival.domain.university.entity.University;
import com.skulikelion.festival.domain.university.enums.Region;

/**
 * {@link AuthPrincipal} 테스트입니다. (LLD-0001 13.1 B그룹)
 *
 * @since 2026.09.14
 */
@DisplayName("AuthPrincipal은")
class AuthPrincipalTest {

  private static Manager manager(Role role) {
    University university = University.builder().id(1L).name("서경대학교").region(Region.SEOUL).build();
    Department department =
        Department.builder().id(12L).university(university).name("소프트웨어학과").build();
    return Manager.builder()
        .id(1024L)
        .department(department)
        .password("encoded")
        .role(role)
        .build();
  }

  @Test
  @DisplayName("매니저로부터 managerId·departmentId·universityId·role을 채운다")
  void from_populatesAllFieldsFromManager() {
    AuthPrincipal principal = AuthPrincipal.from(manager(Role.BOOTH_MANAGER));

    assertThat(principal.managerId()).isEqualTo(1024L);
    assertThat(principal.departmentId()).isEqualTo(12L);
    assertThat(principal.universityId()).isEqualTo(1L);
    assertThat(principal.role()).isEqualTo(Role.BOOTH_MANAGER);
  }

  @Test
  @DisplayName("역할이 ADMIN이면 isAdmin이 true다")
  void isAdmin_isTrue_whenRoleIsAdmin() {
    assertThat(AuthPrincipal.from(manager(Role.ADMIN)).isAdmin()).isTrue();
  }

  @Test
  @DisplayName("역할이 ADMIN이 아니면 isAdmin이 false다")
  void isAdmin_isFalse_whenRoleIsNotAdmin() {
    assertThat(AuthPrincipal.from(manager(Role.BOOTH_MANAGER)).isAdmin()).isFalse();
  }
}
