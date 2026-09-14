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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final ManagerRepository managerRepository;

  /**
   * [ username으로 사용자를 조회하는 메서드 ]
   *
   * <p>여기서 말하는 username은 <b>{@code manager.id}의 문자열 표현</b>이다. {@code UserDetailsService}가 단일 문자열만
   * 받기 때문에, (대학, 학과) 복합 자격을 이미 매니저 식별자로 변환해 넘긴 것이다. (ADR-0001 옵션 2)
   *
   * @param username 관리자 식별자의 문자열 표현
   * @return 조회된 관리자를 감싼 {@link CustomUserDetails}
   * @throws UsernameNotFoundException 식별자가 숫자가 아니거나 해당 관리자가 없을 경우
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Long managerId = parseManagerId(username);
    Manager manager =
        managerRepository
            .findById(managerId)
            .orElseThrow(
                () -> {
                  log.info("[Auth] Security: 해당 식별자를 가진 관리자가 없습니다. - 관리자 식별자: {}", managerId);
                  return new UsernameNotFoundException(username);
                });
    return new CustomUserDetails(manager);
  }

  private Long parseManagerId(String username) {
    try {
      return Long.valueOf(username);
    } catch (NumberFormatException e) {
      log.warn("[Auth] Security: 관리자 식별자 형식이 아닌 username 입력");
      throw new UsernameNotFoundException(username);
    }
  }
}
