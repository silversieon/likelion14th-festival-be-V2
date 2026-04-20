/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.skulikelion.festival.domain.lostitem.dto.response.LostItemResponse;
import com.skulikelion.festival.domain.lostitem.service.LostItemDevService;
import com.skulikelion.festival.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dev/lost-items")
@Tag(name = "Lost Item Dev Controller", description = "개발자용 분실물 조회 API (Soft Delete 포함)")
public class LostItemDevController {

  private final LostItemDevService lostItemDevService;

  @Operation(summary = "[개발자] 삭제된 분실물 전체 조회", description = "Soft Delete된 분실물 목록을 확인하는 API")
  @GetMapping("/deleted")
  public ResponseEntity<BaseResponse<List<LostItemResponse>>> getDeletedItems() {
    List<LostItemResponse> result = lostItemDevService.findDeletedItems();
    return ResponseEntity.ok(BaseResponse.success("삭제된 분실물 조회 성공", result));
  }

  @Operation(summary = "[개발자] 삭제된 분실물 복구", description = "Soft Delete된 분실물을 복구하는 API")
  @PutMapping("/{id}/restore")
  public ResponseEntity<BaseResponse<LostItemResponse>> restoreLostItem(@PathVariable Long id) {
    LostItemResponse response = lostItemDevService.restore(id);
    return ResponseEntity.ok(BaseResponse.success("분실물 복구 성공", response));
  }
}
