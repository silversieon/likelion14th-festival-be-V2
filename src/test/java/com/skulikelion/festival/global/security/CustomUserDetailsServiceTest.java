/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.domain.manager.repository.ManagerRepository;
import com.skulikelion.festival.domain.university.entity.Department;
import com.skulikelion.festival.domain.university.entity.University;
import com.skulikelion.festival.domain.university.enums.Region;

/**
 * {@link CustomUserDetailsService} 테스트입니다. (LLD-0001 13.1 F그룹)
 *
 * @since 2026.09.14
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService는")
class CustomUserDetailsServiceTest {

  @Mock private ManagerRepository managerRepository;

  @InjectMocks private CustomUserDetailsService customUserDetailsService;

  private static Manager manager() {
    University university = University.builder().id(1L).name("서경대학교").region(Region.SEOUL).build();
    Department department =
        Department.builder().id(12L).university(university).name("소프트웨어학과").build();
    return Manager.builder()
        .id(1024L)
        .department(department)
        .password("encoded")
        .role(Role.BOOTH_MANAGER)
        .build();
  }

  @Test
  @DisplayName("username을 관리자 식별자로 해석해 조회한다")
  void loadUserByUsername_findsManagerById() {
    given(managerRepository.findById(1024L)).willReturn(Optional.of(manager()));

    UserDetails userDetails = customUserDetailsService.loadUserByUsername("1024");

    assertThat(userDetails.getUsername()).isEqualTo("1024");
    assertThat(((CustomUserDetails) userDetails).toPrincipal().departmentId()).isEqualTo(12L);
  }

  @Test
  @DisplayName("해당 식별자의 관리자가 없으면 UsernameNotFoundException을 던진다")
  void loadUserByUsername_throws_whenManagerNotFound() {
    given(managerRepository.findById(1024L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("1024"))
        .isInstanceOf(UsernameNotFoundException.class);
  }

  @Test
  @DisplayName("username이 식별자 형식이 아니면 UsernameNotFoundException을 던진다")
  void loadUserByUsername_throws_whenUsernameIsNotNumeric() {
    assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("SOFTWARE"))
        .isInstanceOf(UsernameNotFoundException.class);
  }
}
