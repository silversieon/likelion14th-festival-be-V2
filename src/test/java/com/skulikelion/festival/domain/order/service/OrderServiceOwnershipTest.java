/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.domain.booth.repository.BoothRepository;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.domain.order.entity.enums.OrderStatus;
import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.domain.order.mapper.OrderEventMapper;
import com.skulikelion.festival.domain.order.mapper.OrderMapper;
import com.skulikelion.festival.domain.order.repository.OrderItemRepository;
import com.skulikelion.festival.domain.order.repository.OrderItemUnitRepository;
import com.skulikelion.festival.domain.order.repository.OrderRepository;
import com.skulikelion.festival.domain.order.service.processor.OrderProcessor;
import com.skulikelion.festival.domain.university.entity.Department;
import com.skulikelion.festival.domain.university.entity.University;
import com.skulikelion.festival.domain.university.enums.Region;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.security.AuthPrincipal;
import com.skulikelion.festival.global.security.BoothOwnershipValidator;

/**
 * {@link OrderServiceImpl}의 부스 소유권 경로 테스트입니다. (LLD-0001 13.1 G그룹)
 *
 * <p>토큰 클레임의 {@code departmentId}만으로 부스를 찾고 소유권을 판정하는지 검증한다. 전국 확장 이전에는 {@code Department} enum
 * 문자열을 파싱해 매니저를 조회한 뒤 비교했다. (ADR-0001)
 *
 * @since 2026.09.14
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl의 부스 소유권 검증은")
class OrderServiceOwnershipTest {

  private static final Long OWNER_DEPARTMENT_ID = 12L;
  private static final Long OTHER_DEPARTMENT_ID = 99L;

  @Mock private OrderRepository orderRepository;
  @Mock private OrderMapper orderMapper;
  @Mock private OrderItemRepository orderItemRepository;
  @Mock private BoothRepository boothRepository;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private OrderEventMapper orderEventMapper;
  @Mock private OrderItemUnitRepository orderItemUnitRepository;
  @Mock private OrderProcessor orderProcessor;

  @Spy private BoothOwnershipValidator boothOwnershipValidator = new BoothOwnershipValidator();

  @InjectMocks private OrderServiceImpl orderService;

  private static Booth boothOwnedBy(Long departmentId) {
    University university = University.builder().id(1L).name("서경대학교").region(Region.SEOUL).build();
    Department department =
        Department.builder().id(departmentId).university(university).name("소프트웨어학과").build();
    return Booth.builder().id(5L).department(department).build();
  }

  private static AuthPrincipal principal(Long departmentId, Role role) {
    return new AuthPrincipal(1024L, departmentId, 1L, role);
  }

  @Test
  @DisplayName("요청자의 학과에 부스가 없으면 BOOTH_NOT_FOUND를 던진다")
  void throwsBoothNotFound_whenNoBoothForDepartment() {
    given(boothRepository.findByDepartmentId(OWNER_DEPARTMENT_ID)).willReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                orderService.updateOrderStatus(
                    principal(OWNER_DEPARTMENT_ID, Role.BOOTH_MANAGER), 1L, OrderStatus.COOKING))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(BoothErrorCode.BOOTH_NOT_FOUND);
  }

  @Test
  @DisplayName("조회된 부스의 학과가 요청자의 학과와 다르면 BOOTH_ACCESS_DENIED를 던진다")
  void throwsBoothAccessDenied_whenDepartmentDiffers() {
    given(boothRepository.findByDepartmentId(OTHER_DEPARTMENT_ID))
        .willReturn(Optional.of(boothOwnedBy(OWNER_DEPARTMENT_ID)));

    assertThatThrownBy(
            () ->
                orderService.updateOrderStatus(
                    principal(OTHER_DEPARTMENT_ID, Role.BOOTH_MANAGER), 1L, OrderStatus.COOKING))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(OrderErrorCode.BOOTH_ACCESS_DENIED);
  }

  @Test
  @DisplayName("주문 경로에서는 ADMIN도 다른 학과의 부스에 접근할 수 없다 (기존 동작 보존)")
  void throwsBoothAccessDenied_evenForAdmin() {
    given(boothRepository.findByDepartmentId(OTHER_DEPARTMENT_ID))
        .willReturn(Optional.of(boothOwnedBy(OWNER_DEPARTMENT_ID)));

    assertThatThrownBy(
            () ->
                orderService.updateOrderStatus(
                    principal(OTHER_DEPARTMENT_ID, Role.ADMIN), 1L, OrderStatus.COOKING))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(OrderErrorCode.BOOTH_ACCESS_DENIED);
  }
}
