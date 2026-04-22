/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.manager.entity;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.global.common.BaseTimeEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Manager extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @JsonIgnore
  @Column(nullable = false)
  private String password;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  private Role role = Role.USER;

  @ManyToOne(fetch = FetchType.LAZY)
  private Booth booth;

  public void updateUsername(String username) {
    this.username = username;
  }

  public void updatePassword(String encodedPassword) {
    this.password = encodedPassword;
  }
}
