/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.security;

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.skulikelion.festival.domain.manager.entity.Manager;

import lombok.Getter;

@Getter
public class CustomUserDetails implements UserDetails {

  private final Manager manager;

  public CustomUserDetails(Manager manager) {
    this.manager = manager;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + manager.getRole().name()));
  }

  @Override
  public @Nullable String getPassword() {
    return manager.getPassword();
  }

  /**
   * Spring Security가 다루는 username입니다. <b>{@code manager.id}의 문자열 표현</b>이다.
   *
   * <p>{@link org.springframework.security.core.userdetails.UserDetailsService}는 단일 문자열만 받으므로 "대학 +
   * 학과" 복합 자격을 그대로 태울 수 없다. 그래서 {@code AuthService}가 먼저 (대학, 학과)로 매니저를 찾아 식별자로 변환한 뒤 인증 매니저에 넘긴다.
   * (ADR-0001 옵션 2의 트레이드오프)
   */
  @Override
  public String getUsername() {
    return String.valueOf(manager.getId());
  }

  public AuthPrincipal toPrincipal() {
    return AuthPrincipal.from(manager);
  }
}
