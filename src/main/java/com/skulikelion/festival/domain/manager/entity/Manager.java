/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.manager.entity;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.domain.university.entity.Department;
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
@Table(
    name = "managers",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_managers_department",
            columnNames = {"department_id"}))
public class Manager extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * 이 관리자가 속한 학과입니다. DB에 UNIQUE가 걸려 있어 <b>학과당 계정은 1개</b>다 (현행 동작 보존).
   *
   * <p>매핑을 {@code @OneToOne}이 아니라 {@code @ManyToOne}으로 둔 이유는, 나중에 그 UNIQUE만 푸는 결정이 나더라도 엔티티와 토큰 구조를
   * 다시 바꾸지 않기 위해서다. (ADR-0001 옵션 2)
   *
   * <p>전국 확장 이전에는 {@code Department} enum 값을 가진 UNIQUE 컬럼이자 <b>로그인 ID</b>였고, {@code
   * booth.department}와 FK 없이 문자열로만 이어져 있었다.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id", nullable = false)
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
