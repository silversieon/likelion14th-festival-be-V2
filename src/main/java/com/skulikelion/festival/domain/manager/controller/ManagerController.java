/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.manager.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.skulikelion.festival.domain.manager.dto.request.CreateManagerRequest;
import com.skulikelion.festival.domain.manager.dto.request.UpdateManagerPasswordRequest;
import com.skulikelion.festival.domain.manager.dto.request.UpdateManagerUsernameRequest;
import com.skulikelion.festival.domain.manager.dto.response.ManagerResponse;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.domain.manager.service.ManagerService;
import com.skulikelion.festival.global.common.BaseResponse;
import com.skulikelion.festival.global.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Manager", description = "관리자 관련 기능을 제공하는 API")
public class ManagerController {

  private final ManagerService managerService;

  @Operation(
      summary = "[ 총 관리자 | 토큰 O | 관리자 생성 ]",
      description =
          """
                    **Parameters** \n
                    username: 관리자 아이디 \n
                    password: 관리자 비밀번호 \n
                    role: 관리자 역할(권한- ADMIN, BOOTH_MANAGER, STUDENT_COUNCIL, USER) \n
                    \n
                    **Returns** \n
                    managerId: 관리자 식별자 \n
                    username: 관리자 아이디 \n
                    role: 관리자 역할 \n
                    boothName: 부스명 \n
                    """)
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/admin/managers")
  public ResponseEntity<BaseResponse<ManagerResponse>> createManager(
      @Valid @RequestBody CreateManagerRequest request) {
    ManagerResponse manager = managerService.createManager(request);
    return ResponseEntity.status(201).body(BaseResponse.success(200, "관리자 생성 성공", manager));
  }

  @Operation(
      summary = "[ 총 관리자 | 토큰 O | 관리자 전체 조회 ]",
      description =
          """
                    **Returns** \n
                    모든 관리자 리스트 \n
                    managerId: 관리자 식별자 \n
                    username: 관리자 아이디 \n
                    role: 관리자 역할 \n
                    boothName: 부스명 \n
                    """)
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/admin/managers")
  public ResponseEntity<BaseResponse<List<ManagerResponse>>> getManagers(@RequestParam Role role) {
    List<ManagerResponse> managers = managerService.getManagers(role);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "관리자 전체 조회 성공", managers));
  }

  @Operation(
      summary = "[ 총 관리자 | 토큰 O | 관리자 단건 조회 ]",
      description =
          """
                    **Returns** \n
                    managerId: 관리자 식별자 \n
                    username: 관리자 아이디 \n
                    role: 관리자 역할 \n
                    boothName: 부스명 \n
                    """)
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/admin/managers/{managerId}")
  public ResponseEntity<BaseResponse<ManagerResponse>> getManager(@PathVariable Long managerId) {
    ManagerResponse manager = managerService.getManager(managerId);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "관리자 단건 조회 성공", manager));
  }

  @Operation(
      summary = "[ 총 관리자 | 토큰 O | 관리자 아이디 변경 ]",
      description =
          """
                    **Returns** \n
                    managerId: 관리자 식별자 \n
                    username: 관리자 아이디 \n
                    role: 관리자 역할 \n
                    boothName: 부스명 \n
                    """)
  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/admin/managers/{managerId}/username")
  public ResponseEntity<BaseResponse<ManagerResponse>> updateManagerUsername(
      @PathVariable Long managerId, @RequestBody UpdateManagerUsernameRequest request) {
    ManagerResponse manager = managerService.updateManagerUsername(managerId, request);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "관리자 아이디 변경 성공", manager));
  }

  @Operation(
      summary = "[ 총 관리자 | 토큰 O | 관리자 비밀번호 변경 ]",
      description =
          """
                    **Returns** \n
                    managerId: 관리자 식별자 \n
                    username: 관리자 아이디 \n
                    role: 관리자 역할 \n
                    boothName: 부스명 \n
                    """)
  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/admin/managers/{managerId}/password")
  public ResponseEntity<BaseResponse<ManagerResponse>> updateManagerPassword(
      @PathVariable Long managerId, @RequestBody UpdateManagerPasswordRequest request) {
    ManagerResponse manager = managerService.updateManagerPassword(managerId, request);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "관리자 비밀번호 변경 성공", manager));
  }

  @Operation(
      summary = "[ 총 관리자 | 토큰 O | 관리자 삭제 ]",
      description =
          """
                    **Returns** \n
                    managerId: 관리자 식별자 \n
                    username: 관리자 아이디 \n
                    role: 관리자 역할 \n
                    boothName: 부스명 \n
                    """)
  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/admin/managers/{managerId}")
  public ResponseEntity<BaseResponse<Void>> deleteManager(@PathVariable Long managerId) {
    managerService.deleteManager(managerId);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "관리자 삭제 성공", null));
  }

  @Operation(
      summary = "[ 관리자 | 토큰 O | 본인 정보 조회 ]",
      description =
          """
                            **Returns** \n
                            managerId: 관리자 식별자 \n
                            username: 관리자 아이디 \n
                            role: 관리자 역할 \n
                            boothName: 부스명 \n
                            """)
  @PreAuthorize("hasAnyRole({'ADMIN', 'BOOTH_MANAGER', 'STUDENT_COUNCIL'})")
  @GetMapping("/managers/me")
  public ResponseEntity<BaseResponse<ManagerResponse>> getMyInfo(
      @AuthenticationPrincipal CustomUserDetails customUserDetails) {
    ManagerResponse manager = managerService.getMyInfo(customUserDetails.getManager());
    return ResponseEntity.status(200).body(BaseResponse.success(200, "관리자 본인 정보 조회 성공", manager));
  }
}
