package com.skulikelion.festival.domain.booth.controller;

import com.skulikelion.festival.global.common.BaseResponse;
import com.skulikelion.festival.domain.booth.dto.request.BoothLoginRequest;
import com.skulikelion.festival.domain.booth.dto.response.BoothResponse;
import com.skulikelion.festival.domain.booth.entity.OpeningHours;
import com.skulikelion.festival.domain.booth.service.BoothService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/booths")
@Tag(name = "Booth", description = "Booth 관리 API")
public class BoothController {

  private final BoothService boothService;

  @Operation(summary = "[사용자] 전체 부스 조회", description = "등록된 모든 부스를 조회합니다. (200 OK)")
  @GetMapping
  public ResponseEntity<BaseResponse<List<BoothResponse>>> getAllBooths() {
    List<BoothResponse> boothList = boothService.getAllBooths();
    return ResponseEntity.ok(BaseResponse.success("전체 부스 목록 조회에 성공했습니다.", boothList));
  }

  @Operation(summary = "[사용자] 부스 운영 시간 조회", description = "부스 아이디로 부스의 운영 시간을 조회합니다. (200 OK)")
  @GetMapping("/{id}")
  public ResponseEntity<BaseResponse<OpeningHours>> getBoothOpeningHoursById(
      @Parameter(description = "운영 시간을 조회할 부스 식별자", example = "1") @PathVariable Long id) {
    OpeningHours openingHours = boothService.getBoothOpeningHoursById(id);
    return ResponseEntity.ok(BaseResponse.success("부스 운영 시간 조회에 성공했습니다.", openingHours));
  }

  @Operation(
      summary = "[관리자] 부스 관리자 로그인",
      description = "부스 관리자가 부스 이름과 비밀번호를 입력하여 로그인합니다. 성공 시 부스 정보가 반환됩니다. (200 OK)")
  @PostMapping("/admin/login")
  public ResponseEntity<BaseResponse<BoothResponse>> login(
          @RequestBody @Valid BoothLoginRequest boothLoginRequest, HttpServletRequest request) {
    BoothResponse response = boothService.login(boothLoginRequest, request);
    return ResponseEntity.ok(BaseResponse.success("로그인 성공", response));
  }

  @Operation(
      summary = "[관리자] 부스 대기 팀 수 조회",
      description = "부스 식별자를 기반으로 현재 대기 중인 팀 수를 조회합니다. (200 OK)")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/admin/{id}")
  public int getWaitingTeamCount(
      @Parameter(description = "대기 팀 수를 조회할 부스 식별자", example = "ㅂ") @PathVariable Long id) {
    return boothService.getWaitingTeamCount(id);
  }
}
