/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.controller;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.skulikelion.festival.domain.booth.dto.request.booth.BoothMultipartBody;
import com.skulikelion.festival.domain.booth.dto.request.booth.BoothOperationRequest;
import com.skulikelion.festival.domain.booth.dto.request.booth.BoothRequest;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothAccountResponse;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothBusinessInfoResponse;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothListResponse;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothOperationResponse;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothResponse;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothThumbnailResponse;
import com.skulikelion.festival.domain.booth.enums.BoothLocation;
import com.skulikelion.festival.domain.booth.enums.BoothStatus;
import com.skulikelion.festival.domain.booth.service.booth.BoothService;
import com.skulikelion.festival.global.common.BaseResponse;
import com.skulikelion.festival.global.enums.Language;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Booth", description = "부스 관련 기능을 제공하는 API")
public class BoothController {

  private final BoothService boothService;

  @Operation(
      summary = "[ 총 관리자 | 토큰 O | 부스 생성 ]",
      description =
          """
          **Parameters**  \n
          request: 부스 생성 정보, 운영 정보 1개 이상, KO/EN/ZH 번역 정보 \n
          thumbnail: 부스 썸네일 이미지 \n
          detailImages: 부스 상세 이미지 리스트, 최대 3개 \n
          \n
          **Returns**  \n
          boothId: 부스 식별자 \n
          thumbnailUrl: 부스 썸네일 이미지 URL \n
          open: 영업중 여부 \n
          orderAvailable: 주문 버튼 활성화 여부 \n
          location: 부스 위치 \n
          locationDescription: 부스 위치 설명 \n
          boothNumbers: 부스 번호 목록 \n
          departmentName: 학과명 \n
          boothName: 부스명 \n
          description: 부스 설명 \n
          detailImages: 부스 상세 이미지 리스트 \n
          menus: 낮/밤 메뉴 리스트 \n
          """,
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @Content(
                      mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                      schema = @Schema(implementation = BoothMultipartBody.class),
                      encoding = {
                        @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)
                      })))
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping(value = "/booths", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BaseResponse<BoothResponse>> createBooth(
      @Valid @RequestPart("request") BoothRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
      @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages) {
    BoothResponse response = boothService.createBooth(request, thumbnail, detailImages);
    return ResponseEntity.status(201).body(BaseResponse.success(201, "부스 생성 성공", response));
  }

  @Operation(
      summary = "[ 총 관리자 | 토큰 O | 부스 수정 ]",
      description =
          """
          **Parameters**  \n
          boothId: 수정할 부스 식별자 \n
          request: 부스 수정 정보, 운영 정보 1개 이상, KO/EN/ZH 번역 정보 \n
          thumbnail: 부스 썸네일 이미지 \n
          detailImages: 부스 상세 이미지 리스트, 최대 3개 \n
          \n
          **Returns**  \n
          boothId: 부스 식별자 \n
          thumbnailUrl: 부스 썸네일 이미지 URL \n
          open: 영업중 여부 \n
          orderAvailable: 주문 버튼 활성화 여부 \n
          location: 부스 위치 \n
          locationDescription: 부스 위치 설명 \n
          boothNumbers: 부스 번호 목록 \n
          departmentName: 학과명 \n
          boothName: 부스명 \n
          description: 부스 설명 \n
          detailImages: 부스 상세 이미지 리스트 \n
          menus: 낮/밤 메뉴 리스트 \n
          """,
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @Content(
                      mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                      schema = @Schema(implementation = BoothMultipartBody.class),
                      encoding = {
                        @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)
                      })))
  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping(value = "/booths/{boothId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BaseResponse<BoothResponse>> updateBooth(
      @PathVariable Long boothId,
      @Valid @RequestPart("request") BoothRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
      @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages) {
    BoothResponse response = boothService.updateBooth(boothId, request, thumbnail, detailImages);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "부스 수정 성공", response));
  }

  @Operation(
      summary = "[ 총 관리자 | 토큰 O | 부스 썸네일 수정 ]",
      description =
          """
          **Parameters**  \n
          boothId: 썸네일을 수정할 부스 식별자 \n
          thumbnail: 새 부스 썸네일 이미지 \n
          \n
          **Returns**  \n
          boothId: 부스 식별자 \n
          thumbnailUrl: 수정된 썸네일 이미지 URL \n
          """)
  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping(
      value = "/booths/{boothId}/thumbnail",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BaseResponse<BoothThumbnailResponse>> updateBoothThumbnail(
      @PathVariable Long boothId, @RequestPart("thumbnail") MultipartFile thumbnail) {
    BoothThumbnailResponse response = boothService.updateBoothThumbnail(boothId, thumbnail);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "부스 썸네일 수정 성공", response));
  }

  @Operation(
      summary = "[ 부스 관리자 | 토큰 O | 부스 운영 시간 변경 ]",
      description =
          """
          **Parameters**  \n
          boothId: 운영 시간을 변경할 부스 식별자 \n
          operationDate: 변경할 운영 날짜 \n
          timeType: 운영 시간 타입 \n
          dayOpenTime: 낮 시작 시간, DAY/ALL 필수 \n
          nightOpenTime: 밤 시작 시간, NIGHT/ALL 필수 \n
          closeTime: 마감 시간 \n
          \n
          **Returns**  \n
          operationDate: 운영 날짜 \n
          timeType: 운영 시간 타입 \n
          dayOpenTime: 낮 시작 시간 \n
          nightOpenTime: 밤 시작 시간 \n
          closeTime: 마감 시간 \n
          """)
  @PreAuthorize("hasAnyRole({'ADMIN', 'BOOTH_MANAGER'})")
  @PatchMapping("/booths/{boothId}/operations")
  public ResponseEntity<BaseResponse<BoothOperationResponse>> updateBoothOperation(
      @AuthenticationPrincipal String departmentName,
      @PathVariable Long boothId,
      @Valid @RequestBody BoothOperationRequest request) {
    BoothOperationResponse response =
        boothService.updateBoothOperation(departmentName, boothId, request);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "부스 운영 시간 변경 성공", response));
  }

  @Operation(
      summary = "[ 부스 관리자 | 토큰 O | 부스 운영 시간 정보 조회 ]",
      description =
          """
          **Parameters**  \n
          boothId: 운영 시간 정보를 조회할 부스 식별자 \n
          \n
          **Returns**  \n
          operationDate: 운영 날짜 \n
          timeType: 운영 시간 타입 \n
          dayOpenTime: 낮 시작 시간 \n
          nightOpenTime: 밤 시작 시간 \n
          closeTime: 마감 시간 \n
          """)
  @PreAuthorize("hasAnyRole({'ADMIN', 'BOOTH_MANAGER'})")
  @GetMapping("/booths/{boothId}/operations")
  public ResponseEntity<BaseResponse<List<BoothOperationResponse>>> getBoothOperationInfos(
      @AuthenticationPrincipal String departmentName, @PathVariable Long boothId) {
    List<BoothOperationResponse> response =
        boothService.getBoothOperationInfos(departmentName, boothId);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "부스 정보 조회 성공", response));
  }

  @Operation(
      summary = "[ 총 관리자 | 토큰 O | 부스 삭제 ]",
      description =
          """
          **Parameters**  \n
          boothId: 삭제할 부스 식별자 \n
          \n
          **Returns**  \n
          부스 삭제 성공 여부 \n
          """)
  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/booths/{boothId}")
  public ResponseEntity<BaseResponse<Void>> deleteBooth(@PathVariable Long boothId) {
    boothService.deleteBooth(boothId);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "부스 삭제 성공", null));
  }

  @Operation(
      summary = "[ 사용자 | 토큰 X | 부스 전체/위치별 조회 ]",
      description =
          """
          **Parameters**  \n
          location: 조회할 부스 위치, 미입력 시 전체 조회 \n
          \n
          **Returns**  \n
          boothId: 부스 식별자 \n
          thumbnailUrl: 부스 썸네일 이미지 URL \n
          location: 부스 위치 \n
          locationDescription: 부스 위치 설명 \n
          boothNumbers: 부스 번호 목록 \n
          departmentName: 학과명 \n
          """)
  @GetMapping("/booths")
  public ResponseEntity<BaseResponse<List<BoothListResponse>>> getBooths(
      @RequestParam(required = false) BoothLocation location) {
    List<BoothListResponse> responses = boothService.getBooths(location);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "부스 목록 조회 성공", responses));
  }

  @Operation(
      summary = "[ 사용자 | 토큰 X | 부스 검색 ]",
      description =
          """
          **Parameters**  \n
          keyword: 검색할 학과명 키워드 \n
          \n
          **Returns**  \n
          boothId: 부스 식별자 \n
          thumbnailUrl: 부스 썸네일 이미지 URL \n
          location: 부스 위치 \n
          locationDescription: 부스 위치 설명 \n
          boothNumbers: 부스 번호 목록 \n
          departmentName: 학과명 \n
          """)
  @GetMapping("/booths/search")
  public ResponseEntity<BaseResponse<List<BoothListResponse>>> searchBooths(
      @RequestParam String keyword) {
    List<BoothListResponse> responses = boothService.searchBooths(keyword);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "부스 검색 성공", responses));
  }

  @Operation(
      summary = "[ 사용자 | 토큰 X | 부스 단건 조회 ]",
      description =
          """
          **Parameters**  \n
          boothId: 조회할 부스 식별자 \n
          lang: 조회할 번역 언어 \n
          \n
          **Returns**  \n
          boothId: 부스 식별자 \n
          thumbnailUrl: 부스 썸네일 이미지 URL \n
          open: 영업중 여부 \n
          orderAvailable: 주문 버튼 활성화 여부 \n
          location: 부스 위치 \n
          locationDescription: 부스 위치 설명 \n
          boothNumbers: 부스 번호 목록 \n
          departmentName: 학과명 \n
          boothName: 부스명 \n
          description: 부스 설명 \n
          detailImages: 부스 상세 이미지 리스트 \n
          menus: 낮/밤 메뉴 리스트 \n
          """)
  @GetMapping("/booths/{boothId}")
  public ResponseEntity<BaseResponse<BoothResponse>> getBooth(
      @PathVariable Long boothId, @RequestParam Language lang) {
    BoothResponse response = boothService.getBooth(boothId, lang);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "부스 단건 조회 성공", response));
  }

  @Operation(
      summary = "[ 사용자 | 토큰 X | 입금 계좌 조회 ]",
      description =
          """
          **Parameters**  \n
          boothId: 조회할 부스 식별자 \n
          \n
          **Returns**  \n
          accountName: 예금주 \n
          accountNumber: 계좌번호 \n
          bankName: 은행명 \n
          """)
  @GetMapping("/booths/{boothId}/account")
  public ResponseEntity<BaseResponse<BoothAccountResponse>> getBoothAccount(
      @PathVariable Long boothId) {
    BoothAccountResponse response = boothService.getBoothAccount(boothId);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "입금 계좌 조회 성공", response));
  }

  @Operation(
      summary = "[ 부스 관리자 | 토큰 O | 부스 영업 관리 조회 ]",
      description =
          """
              **Parameters**  \n
              date: 매출을 조회할 날짜 (2026-05-01 형태) \n
              \n
              **Returns**  \n
              departmentName: 학과명 \n
              isActive: 부스 영업 중 여부 \n
              dayOpenTime: 낮 오픈 시간 \n
              nightOpenTime: 밤 오픈 시간 \n
              closeTime: 마감 시간 \n
              sales: 매출 \n
          """)
  @PreAuthorize("hasRole('BOOTH_MANAGER')")
  @GetMapping("/booths/business-info")
  public ResponseEntity<BaseResponse<BoothBusinessInfoResponse>> getBoothBusinessInfo(
      @AuthenticationPrincipal String departmentName,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date) {
    BoothBusinessInfoResponse boothBusinessInfo =
        boothService.getBoothBusinessInfo(departmentName, date);
    return ResponseEntity.status(200)
        .body(BaseResponse.success(200, "부스 영업 정보 조회 성공", boothBusinessInfo));
  }

  @Operation(
      summary = "[ 부스 관리자 | 토큰 O | 부스 영업 중으로 전환 ]",
      description = """
          **Returns**  \n
          """)
  @PreAuthorize("hasRole('BOOTH_MANAGER')")
  @PatchMapping("/booths/status/open")
  public ResponseEntity<BaseResponse<Void>> changeBoothStatusToOpen(
      @AuthenticationPrincipal String departmentName) {
    boothService.changeBoothStatusToOpen(departmentName);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "부스 영업 중 전환 성공", null));
  }

  @Operation(
      summary = "[ 부스 관리자 | 토큰 O | 부스 영업 중단으로 전환 ]",
      description =
          """
          **Parameters**  \n
          BoothStatus: 중단 사유 (OPEN은 예외 처리 됨)
          \n
          **Returns**  \n
          """)
  @PreAuthorize("hasRole('BOOTH_MANAGER')")
  @PatchMapping("/booths/status/close")
  public ResponseEntity<BaseResponse<Void>> getBoothBusinessInfo(
      @AuthenticationPrincipal String departmentName, @RequestParam BoothStatus boothStatus) {
    boothService.changeBoothStatusToClose(departmentName, boothStatus);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "부스 영업 중단 전환 성공", null));
  }
}
