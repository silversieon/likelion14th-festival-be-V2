/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.domain.university.entity.Department;
import com.skulikelion.festival.domain.university.entity.University;
import com.skulikelion.festival.domain.university.enums.Region;
import com.skulikelion.festival.global.exception.CustomException;

/**
 * {@link BoothOwnershipValidator} 테스트입니다. (LLD-0001 13.1 C그룹)
 *
 * <p>C4는 주문 경로에 ADMIN 우회가 없는 <b>기존 동작을 보존</b>하는 것이 목적이다 (LLD-0001 2.3 R6).
 *
 * @since 2026.09.14
 */
@DisplayName("BoothOwnershipValidator는")
class BoothOwnershipValidatorTest {

  private static final Long OWNER_DEPARTMENT_ID = 12L;
  private static final Long OTHER_DEPARTMENT_ID = 99L;

  private final BoothOwnershipValidator validator = new BoothOwnershipValidator();

  private static Booth boothOwnedBy(Long departmentId) {
    University university = University.builder().id(1L).name("서경대학교").region(Region.SEOUL).build();
    Department department =
        Department.builder().id(departmentId).university(university).name("소프트웨어학과").build();
    return Booth.builder().id(5L).department(department).build();
  }

  private static AuthPrincipal principal(Long departmentId, Role role) {
    return new AuthPrincipal(1024L, departmentId, 1L, role);
  }

  @Nested
  @DisplayName("validateOwnerOrAdmin은")
  class ValidateOwnerOrAdmin {

    @Test
    @DisplayName("요청자의 학과가 부스의 학과와 같으면 통과시킨다")
    void passes_whenDepartmentMatches() {
      assertThatCode(
              () ->
                  validator.validateOwnerOrAdmin(
                      principal(OWNER_DEPARTMENT_ID, Role.BOOTH_MANAGER),
                      boothOwnedBy(OWNER_DEPARTMENT_ID),
                      BoothErrorCode.BOOTH_ACCESS_DENIED))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("학과가 달라도 ADMIN이면 통과시킨다")
    void passes_whenRoleIsAdmin() {
      assertThatCode(
              () ->
                  validator.validateOwnerOrAdmin(
                      principal(OTHER_DEPARTMENT_ID, Role.ADMIN),
                      boothOwnedBy(OWNER_DEPARTMENT_ID),
                      BoothErrorCode.BOOTH_ACCESS_DENIED))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("학과가 다르고 ADMIN도 아니면 전달받은 에러 코드로 예외를 던진다")
    void throwsGivenErrorCode_whenNotOwnerAndNotAdmin() {
      assertThatThrownBy(
              () ->
                  validator.validateOwnerOrAdmin(
                      principal(OTHER_DEPARTMENT_ID, Role.BOOTH_MANAGER),
                      boothOwnedBy(OWNER_DEPARTMENT_ID),
                      BoothErrorCode.BOOTH_ACCESS_DENIED))
          .isInstanceOf(CustomException.class)
          .extracting(e -> ((CustomException) e).getErrorCode())
          .isEqualTo(BoothErrorCode.BOOTH_ACCESS_DENIED);
    }
  }

  @Nested
  @DisplayName("validateOwner는")
  class ValidateOwner {

    @Test
    @DisplayName("요청자의 학과가 부스의 학과와 같으면 통과시킨다")
    void passes_whenDepartmentMatches() {
      assertThatCode(
              () ->
                  validator.validateOwner(
                      principal(OWNER_DEPARTMENT_ID, Role.BOOTH_MANAGER),
                      boothOwnedBy(OWNER_DEPARTMENT_ID),
                      OrderErrorCode.BOOTH_ACCESS_DENIED))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("학과가 다르면 ADMIN이어도 예외를 던진다 (주문 경로의 기존 동작 보존)")
    void throws_evenWhenRoleIsAdmin() {
      assertThatThrownBy(
              () ->
                  validator.validateOwner(
                      principal(OTHER_DEPARTMENT_ID, Role.ADMIN),
                      boothOwnedBy(OWNER_DEPARTMENT_ID),
                      OrderErrorCode.BOOTH_ACCESS_DENIED))
          .isInstanceOf(CustomException.class)
          .extracting(e -> ((CustomException) e).getErrorCode())
          .isEqualTo(OrderErrorCode.BOOTH_ACCESS_DENIED);
    }
  }
}
