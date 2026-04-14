package com.skulikelion.festival.domain.booth.controller;

import com.skulikelion.festival.domain.booth.dto.request.BoothMenuRequest;
import com.skulikelion.festival.domain.booth.dto.response.BoothMenuResponse;
import com.skulikelion.festival.domain.booth.service.BoothMenuService;
import com.skulikelion.festival.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dev/booths/menus")
@Tag(name = "BoothDev", description = "개발자용 BoothMenu 관리 API")
public class BoothMenuController {

  private final BoothMenuService boothMenuService;

  // 메뉴 등록
  @Operation(
      summary = "[개발자] 새 메뉴 등록",
      description = "특정 부스에 새로운 메뉴를 등록하고 생성된 메뉴 정보를 반환합니다. (201 Created)")
  @PostMapping
  public ResponseEntity<BaseResponse<BoothMenuResponse>> createBooth(
      @RequestBody @Valid BoothMenuRequest request) {
    BoothMenuResponse response = boothMenuService.createBoothMenu(request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(BaseResponse.success("메뉴 등록이 완료되었습니다.", response));
  }

  // 메뉴 수정
  @Operation(summary = "[개발자] 부스 메뉴 수정", description = "특정 부스의 메뉴를 수정합니다. (200 OK)")
  @PutMapping
  public ResponseEntity<BaseResponse<BoothMenuResponse>> updateBooth(
      @RequestBody @Valid BoothMenuRequest request) {
    BoothMenuResponse response = boothMenuService.updateBoothMenu(request);
    return ResponseEntity.ok(BaseResponse.success("메뉴 정보가 수정되었습니다.", response));
  }

  // 메뉴 삭제
  @Operation(summary = "[개발자] 메뉴 삭제", description = "생성된 메뉴를 삭제합니다. (200 OK)")
  @DeleteMapping
  public ResponseEntity<BaseResponse<String>> deleteBooth(
      @Parameter(description = "부스 이름", example = "디자인학부") @RequestParam String boothFaculty,
      @Parameter(description = "삭제할 메뉴의 한국어 이름", example = "감자튀김") @RequestParam String menuMame) {
    boothMenuService.deleteBoothMenu(boothFaculty, menuMame);
    return ResponseEntity.ok(BaseResponse.success("메뉴가 삭제되었습니다."));
  }
}
