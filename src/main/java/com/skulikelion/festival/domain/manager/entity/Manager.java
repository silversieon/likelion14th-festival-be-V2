/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.manager.entity;

import jakarta.persistence.*;

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

  private String username;

  private String password;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  private Role role = Role.USER;

  @ManyToOne(fetch = FetchType.LAZY)
  private Booth booth;
}
