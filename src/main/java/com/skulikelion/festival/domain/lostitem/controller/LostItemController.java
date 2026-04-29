/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.skulikelion.festival.domain.lostitem.dto.request.LostItemMultipartBody;
import com.skulikelion.festival.domain.lostitem.dto.request.LostItemRequest;
import com.skulikelion.festival.domain.lostitem.dto.response.LostItemPageResponse;
import com.skulikelion.festival.domain.lostitem.dto.response.LostItemResponse;
import com.skulikelion.festival.domain.lostitem.service.LostItemService;
import com.skulikelion.festival.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lost-items")
@Tag(name = "Lost Item", description = "분실물 관련 기능을 제공하는 API")
public class LostItemController {

  private final LostItemService lostItemService;

  @Operation(
      summary = "[ 토큰 X | 분실물 전체 조회 + 검색/필터 ]",
      description =
          """
          **Parameters**  \n
          name: 분실물 이름 검색어 (선택) \n
          foundDate: 습득 날짜 필터 (선택, 예: 2026-05-13) \n
          page: 페이지 번호 (기본값: 0) \n
          size: 페이지당 게시물 수 (기본값: 4) \n
          \n
          **Returns**  \n
          content: 분실물 리스트 \n
            - id: 분실물 식별자  \n
            - name: 분실물 이름  \n
            - imageUrl: 대표 이미지 URL, 여러 이미지 중 첫 번째로 등록된 이미지  \n
            - foundPlace: 습득 장소  \n
            - foundDate: 습득 날짜  \n
            - dayOfWeek: 습득 요일  \n
          totalElements: 전체 분실물 개수 \n
          totalPages: 전체 페이지 개수 \n
          pageNum: 현재 페이지 번호 \n
          pageSize: 페이지 크기 \n
          last: 마지막 페이지 여부 \n
          """)
  @GetMapping
  public ResponseEntity<BaseResponse<LostItemPageResponse>> getLostItems(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) LocalDate foundDate,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "4") int size) {

    LostItemPageResponse response = lostItemService.getLostItems(name, foundDate, page, size);

    return ResponseEntity.status(200).body(BaseResponse.success(200, "분실물 전체 조회 성공", response));
  }

  @Operation(
      summary = "[ 토큰 X | 분실물 상세 조회 ]",
      description =
          """
          **Parameters**  \n
          lostItemId: 조회할 분실물 식별자 \n
          \n
          **Returns**  \n
          id: 분실물 식별자  \n
          name: 분실물 이름  \n
          imageUrls: 분실물 이미지 URL 리스트  \n
          foundPlace: 습득 장소  \n
          foundDate: 습득 날짜  \n
          dayOfWeek: 습득 요일  \n
          returned: 수령 여부  \n
          """)
  @GetMapping("/{lostItemId}")
  public ResponseEntity<BaseResponse<LostItemResponse>> getLostItem(@PathVariable Long lostItemId) {

    LostItemResponse response = lostItemService.getLostItem(lostItemId);

    return ResponseEntity.status(200).body(BaseResponse.success(200, "분실물 상세 조회 성공", response));
  }

  @Operation(
      summary = "[ 관리자 | 토큰 O | 분실물 등록 ]",
      description =
          """
          **Parameters**  \n
          request: 분실물 등록 정보  \n
          images: 분실물 이미지 리스트, 최소 1장 최대 4장  \n
          \n
          **Returns**  \n
          id: 분실물 식별자  \n
          name: 분실물 이름  \n
          imageUrls: 분실물 이미지 URL 리스트  \n
          foundPlace: 습득 장소  \n
          foundDate: 습득 날짜  \n
          dayOfWeek: 습득 요일  \n
          returned: 수령 여부  \n
          """,
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                      schema = @Schema(implementation = LostItemMultipartBody.class),
                      encoding = {
                        @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)
                      })))
  @PreAuthorize("hasAnyRole('STUDENT_COUNCIL', 'ADMIN')")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BaseResponse<LostItemResponse>> createLostItem(
      @Valid @RequestPart(value = "request", required = true) LostItemRequest request,
      @RequestPart(value = "images", required = true) List<MultipartFile> images) {

    LostItemResponse response = lostItemService.createLostItem(request, images);

    return ResponseEntity.status(201).body(BaseResponse.success(201, "분실물 등록 성공", response));
  }

  @Operation(
      summary = "[ 관리자 | 토큰 O | 분실물 수정 ]",
      description =
          """
          **Parameters**  \n
          lostItemId: 수정할 분실물 식별자 \n
          request: 분실물 수정 정보, 기존 값 포함하여 전달 \n
          images: 분실물 이미지 리스트, 선택값. 전달하지 않으면 기존 이미지 유지, 전달하면 기존 이미지 전체 교체 \n
          \n
          **Returns**  \n
          id: 분실물 식별자 \n
          name: 분실물 이름 \n
          imageUrls: 분실물 이미지 URL 리스트, 등록 순서대로 제공 \n
          foundPlace: 습득 장소 \n
          foundDate: 습득 날짜 \n
          dayOfWeek: 습득 요일 \n
          returned: 수령 여부 \n
          """,
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                      schema = @Schema(implementation = LostItemMultipartBody.class),
                      encoding = {
                        @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)
                      })))
  @PreAuthorize("hasAnyRole('STUDENT_COUNCIL', 'ADMIN')")
  @PutMapping(value = "/{lostItemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BaseResponse<LostItemResponse>> updateLostItem(
      @PathVariable Long lostItemId,
      @Valid @RequestPart(value = "request", required = true) LostItemRequest request,
      @RequestPart(value = "images", required = false) List<MultipartFile> images) {

    LostItemResponse response = lostItemService.updateLostItem(lostItemId, request, images);

    return ResponseEntity.status(200).body(BaseResponse.success(200, "분실물 수정 성공", response));
  }

  @Operation(
      summary = "[ 관리자 | 토큰 O | 분실물 삭제 ]",
      description =
          """
          **Parameters**  \n
          lostItemId: 삭제할 분실물 식별자 \n
          \n
          **Returns**  \n
          삭제 성공 메시지 \n
          """)
  @PreAuthorize("hasAnyRole('STUDENT_COUNCIL', 'ADMIN')")
  @DeleteMapping("/{lostItemId}")
  public ResponseEntity<BaseResponse<Map<String, Long>>> deleteLostItem(
      @PathVariable Long lostItemId) {

    lostItemService.deleteLostItem(lostItemId);

    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "분실물 삭제 성공", Map.of("id", lostItemId)));
  }

  @Operation(
      summary = "[ 관리자 | 토큰 O | 분실물 수령 상태 변경 ]",
      description =
          """
          **Parameters**  \n
          lostItemId: 상태를 변경할 분실물 식별자 \n
          returned: 수령 여부 (true: 수령 처리, false: 미수령 처리) \n
          \n
          **Returns**  \n
          id: 분실물 식별자 \n
          name: 분실물 이름 \n
          imageUrls: 분실물 이미지 URL 리스트 \n
          foundPlace: 습득 장소 \n
          foundDate: 습득 날짜 \n
          dayOfWeek: 습득 요일 \n
          returned: 수령 여부 \n
          """)
  @PreAuthorize("hasAnyRole('STUDENT_COUNCIL', 'ADMIN')")
  @PatchMapping("/{lostItemId}/status")
  public ResponseEntity<BaseResponse<LostItemResponse>> updateLostItemStatus(
      @PathVariable Long lostItemId, @RequestParam boolean returned) {

    LostItemResponse response = lostItemService.updateLostItemStatus(lostItemId, returned);

    return ResponseEntity.status(200).body(BaseResponse.success(200, "분실물 수령 상태 변경 성공", response));
  }
}
