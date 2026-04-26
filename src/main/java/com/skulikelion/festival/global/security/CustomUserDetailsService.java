/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.repository.ManagerRepository;
import com.skulikelion.festival.global.enums.Department;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final ManagerRepository managerRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Manager manager =
        managerRepository
            .findByDepartment(Department.valueOf(username))
            .orElseThrow(
                () -> {
                  log.info("[Auth] Security: 해당 학과명을 가진 사용자가 없습니다. - 학과명: {}", username);
                  return new UsernameNotFoundException(username);
                });
    return new CustomUserDetails(manager);
  }
}
