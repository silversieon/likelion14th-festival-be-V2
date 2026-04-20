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

  @Override
  public String getUsername() {
    return manager.getUsername();
  }
}
