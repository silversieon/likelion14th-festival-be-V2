/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.manager.entity;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.global.common.BaseTimeEntity;
import com.skulikelion.festival.global.enums.Department;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "managers")
public class Manager extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, unique = true)
  private Department department;

  @JsonIgnore
  @Column(nullable = false)
  private String password;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  private Role role = Role.USER;

  public void updatePassword(String encodedPassword) {
    this.password = encodedPassword;
  }
}
