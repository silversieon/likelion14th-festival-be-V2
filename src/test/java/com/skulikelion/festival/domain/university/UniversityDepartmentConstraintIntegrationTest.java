/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.university;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.repository.BoothRepository;
import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.domain.manager.repository.ManagerRepository;
import com.skulikelion.festival.domain.support.IntegrationTestSupport;
import com.skulikelion.festival.domain.university.entity.Department;
import com.skulikelion.festival.domain.university.entity.University;
import com.skulikelion.festival.domain.university.enums.Region;
import com.skulikelion.festival.domain.university.repository.DepartmentRepository;
import com.skulikelion.festival.domain.university.repository.UniversityRepository;

/**
 * 전국 확장 스키마의 DB 제약을 실제 MySQL로 검증하는 통합 테스트입니다. (LLD-0001 13.2 I1~I4, I7)
 *
 * <p>{@code I1}이 이 개편의 핵심 주장을 증명한다 — <b>서로 다른 대학에 같은 이름의 학과가 공존</b>할 수 있어야 한다. 기존 스키마는 {@code
 * booth.department}가 단일 UNIQUE라 이것이 불가능했다. (ADR-0001 결정 동인 1)
 *
 * <p>⚠️ Testcontainers를 쓰므로 Docker Desktop이 실행 중이어야 한다 (AGENTS.md 5장).
 *
 * @since 2026.09.14
 */
@DisplayName("전국 확장 스키마 제약은")
class UniversityDepartmentConstraintIntegrationTest extends IntegrationTestSupport {

  private static final String SAME_DEPARTMENT_NAME = "소프트웨어학과";

  @Autowired private UniversityRepository universityRepository;
  @Autowired private DepartmentRepository departmentRepository;
  @Autowired private BoothRepository boothRepository;
  @Autowired private ManagerRepository managerRepository;

  private University saveUniversity(String name, Region region) {
    return universityRepository.saveAndFlush(
        University.builder().name(name).region(region).build());
  }

  private Department saveDepartment(University university, String name) {
    return departmentRepository.saveAndFlush(
        Department.builder().university(university).name(name).build());
  }

  @Test
  @DisplayName("서로 다른 대학에 같은 이름의 학과를 저장할 수 있다")
  void allowsSameDepartmentNameAcrossUniversities() {
    University a = saveUniversity("가대학교", Region.SEOUL);
    University b = saveUniversity("나대학교", Region.GYEONGGI);

    Department departmentOfA = saveDepartment(a, SAME_DEPARTMENT_NAME);
    Department departmentOfB = saveDepartment(b, SAME_DEPARTMENT_NAME);

    assertThat(departmentOfA.getId()).isNotEqualTo(departmentOfB.getId());
    assertThat(departmentOfA.getName()).isEqualTo(departmentOfB.getName());
  }

  @Test
  @DisplayName("같은 대학에 같은 이름의 학과를 두 번 저장하면 제약 위반이 발생한다")
  void rejectsDuplicateDepartmentNameInSameUniversity() {
    University university = saveUniversity("다대학교", Region.BUSAN);
    saveDepartment(university, SAME_DEPARTMENT_NAME);

    assertThatThrownBy(() -> saveDepartment(university, SAME_DEPARTMENT_NAME))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("같은 학과에 부스를 두 개 저장하면 제약 위반이 발생한다 (1:1 강제)")
  void rejectsSecondBoothForSameDepartment() {
    University university = saveUniversity("라대학교", Region.DAEGU);
    Department department = saveDepartment(university, "경영학부");
    boothRepository.saveAndFlush(Booth.builder().department(department).build());

    assertThatThrownBy(
            () -> boothRepository.saveAndFlush(Booth.builder().department(department).build()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("같은 학과에 매니저를 두 명 저장하면 제약 위반이 발생한다")
  void rejectsSecondManagerForSameDepartment() {
    University university = saveUniversity("마대학교", Region.INCHEON);
    Department department = saveDepartment(university, "영화영상학과");
    managerRepository.saveAndFlush(
        Manager.builder()
            .department(department)
            .password("encoded")
            .role(Role.BOOTH_MANAGER)
            .build());

    assertThatThrownBy(
            () ->
                managerRepository.saveAndFlush(
                    Manager.builder()
                        .department(department)
                        .password("encoded2")
                        .role(Role.BOOTH_MANAGER)
                        .build()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("V14 마이그레이션이 적재한 서경대학교와 학과 33개가 존재한다")
  void migrationSeedsSkuniversityAndDepartments() {
    assertThat(universityRepository.existsByNameAndRegion("서경대학교", Region.SEOUL)).isTrue();

    University sku =
        universityRepository.findAll().stream()
            .filter(u -> u.getName().equals("서경대학교") && u.getRegion() == Region.SEOUL)
            .findFirst()
            .orElseThrow();

    assertThat(departmentRepository.existsByUniversityIdAndName(sku.getId(), "소프트웨어학과")).isTrue();
    assertThat(departmentRepository.existsByUniversityIdAndName(sku.getId(), "멋사 운영진")).isTrue();
    assertThat(
            departmentRepository.findAll().stream()
                .filter(d -> d.getUniversity().getId().equals(sku.getId()))
                .count())
        .isEqualTo(33);
  }
}
