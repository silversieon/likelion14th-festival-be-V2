/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.skulikelion.festival.domain.booth.dto.request.menu.BoothMenuBulkMultipartBody;
import com.skulikelion.festival.domain.booth.dto.request.menu.BoothMenuListRequest;
import com.skulikelion.festival.domain.booth.dto.request.menu.BoothMenuMultipartBody;
import com.skulikelion.festival.domain.booth.dto.request.menu.BoothMenuRequest;
import com.skulikelion.festival.domain.booth.dto.request.menu.UpdateBoothMenuPriceRequest;
import com.skulikelion.festival.domain.booth.dto.request.menu.UpdateBoothMenuSoldOutRequest;
import com.skulikelion.festival.domain.booth.dto.response.menu.BoothMenuResponse;
import com.skulikelion.festival.domain.booth.dto.response.menu.OrderAvailableBoothMenuGroupResponse;
import com.skulikelion.festival.domain.booth.service.menu.BoothMenuService;
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
@Tag(name = "Booth Menu", description = "부스 메뉴 관련 기능을 제공하는 API")
public class BoothMenuController {

  private final BoothMenuService boothMenuService;

  @Operation(
      summary = "[ 총 관리자 | 토큰 O | 메뉴 생성 ]",
      description =
          """
          **Parameters**  \n
          boothId: 메뉴를 생성할 부스 식별자 \n
          request: 메뉴 생성 정보 목록 \n
          iconImages: 메뉴 아이콘 이미지 리스트, 입력 시 request.menus 개수 및 순서와 일치 필요 \n
          \n
          **Returns**  \n
          menuId: 메뉴 식별자 \n
          name: 메뉴명 \n
          price: 가격 \n
          timeType: 운영 시간 타입 \n
          soldOut: 품절 여부 \n
          description: 메뉴 설명 \n
          category: 메뉴 카테고리 \n
          iconImageUrl: 아이콘 이미지 URL \n
          """,
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @Content(
                      mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                      schema = @Schema(implementation = BoothMenuBulkMultipartBody.class),
                      encoding = {
                        @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)
                      })))
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping(value = "/booths/{boothId}/menus", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BaseResponse<List<BoothMenuResponse>>> createMenus(
      @PathVariable Long boothId,
      @Valid @RequestPart("request") BoothMenuListRequest request,
      @RequestPart(value = "iconImages", required = false) List<MultipartFile> iconImages) {
    List<BoothMenuResponse> response =
        boothMenuService.createMenus(boothId, request.getMenus(), iconImages);
    return ResponseEntity.status(201).body(BaseResponse.success(201, "메뉴 생성 성공", response));
  }

  @Operation(
      summary = "[ 사용자 | 토큰 X | 주문 가능 부스 메뉴 조회 ]",
      description =
          """
          **Parameters**  \n
          boothId: 메뉴를 조회할 부스 식별자 \n
          lang: 조회할 메뉴명 언어 \n
          \n
          **Returns**  \n
          main: 메인 메뉴 목록 \n
          side: 사이드 메뉴 목록 \n
          drink: 음료 메뉴 목록 \n
          menuId: 메뉴 식별자 \n
          iconImageUrl: 아이콘 이미지 URL \n
          name: 언어별 메뉴명 \n
          description: 메뉴 설명 \n
          price: 가격 \n
          soldOut: 품절 여부 \n
          """)
  @GetMapping("/booths/{boothId}/menus/order-available")
  public ResponseEntity<BaseResponse<OrderAvailableBoothMenuGroupResponse>> getOrderAvailableMenus(
      @PathVariable Long boothId, @RequestParam Language lang) {
    OrderAvailableBoothMenuGroupResponse response =
        boothMenuService.getOrderAvailableMenus(boothId, lang);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "주문 가능 메뉴 조회 성공", response));
  }

  @Operation(
      summary = "[ 총 관리자 | 토큰 O | 메뉴 수정 ]",
      description =
          """
          **Parameters**  \n
          menuId: 수정할 메뉴 식별자 \n
          request: 메뉴 수정 정보 \n
          iconImage: 메뉴 아이콘 이미지 \n
          \n
          **Returns**  \n
          menuId: 메뉴 식별자 \n
          name: 메뉴명 \n
          price: 가격 \n
          timeType: 운영 시간 타입 \n
          soldOut: 품절 여부 \n
          description: 메뉴 설명 \n
          category: 메뉴 카테고리 \n
          iconImageUrl: 아이콘 이미지 URL \n
          """,
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @Content(
                      mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                      schema = @Schema(implementation = BoothMenuMultipartBody.class),
                      encoding = {
                        @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)
                      })))
  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping(value = "/booth-menus/{menuId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BaseResponse<BoothMenuResponse>> updateMenu(
      @PathVariable Long menuId,
      @Valid @RequestPart("request") BoothMenuRequest request,
      @RequestPart(value = "iconImage", required = false) MultipartFile iconImage) {
    BoothMenuResponse response = boothMenuService.updateMenu(menuId, request, iconImage);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "메뉴 수정 성공", response));
  }

  @Operation(
      summary = "[ 부스 관리자 | 토큰 O | 메뉴 가격 수정 ]",
      description =
          """
          **Parameters**  \n
          departmentName: 학과명 \n
          menuId: 가격을 수정할 메뉴 식별자 \n
          price: 변경할 메뉴 가격 \n
          \n
          **Returns**  \n
          menuId: 메뉴 식별자 \n
          name: 메뉴명 \n
          price: 가격 \n
          timeType: 운영 시간 타입 \n
          soldOut: 품절 여부 \n
          description: 메뉴 설명 \n
          category: 메뉴 카테고리 \n
          iconImageUrl: 아이콘 이미지 URL \n
          """)
  @PreAuthorize("hasAnyRole({'ADMIN', 'BOOTH_MANAGER'})")
  @PatchMapping("/booth-menus/{menuId}/price")
  public ResponseEntity<BaseResponse<BoothMenuResponse>> updateMenuPrice(
      @AuthenticationPrincipal String departmentName,
      @PathVariable Long menuId,
      @Valid @RequestBody UpdateBoothMenuPriceRequest request) {
    BoothMenuResponse response = boothMenuService.updateMenuPrice(departmentName, menuId, request);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "메뉴 가격 수정 성공", response));
  }

  @Operation(
      summary = "[ 부스 관리자 | 토큰 O | 메뉴 품절 여부 변경 ]",
      description =
          """
          **Parameters**  \n
          departmentName: 학과명 \n
          menuId: 품절 여부를 변경할 메뉴 식별자 \n
          soldOut: 변경할 품절 여부 \n
          \n
          **Returns**  \n
          menuId: 메뉴 식별자 \n
          name: 메뉴명 \n
          price: 가격 \n
          timeType: 운영 시간 타입 \n
          soldOut: 품절 여부 \n
          description: 메뉴 설명 \n
          category: 메뉴 카테고리 \n
          iconImageUrl: 아이콘 이미지 URL \n
          """)
  @PreAuthorize("hasAnyRole({'ADMIN', 'BOOTH_MANAGER'})")
  @PatchMapping("/booth-menus/{menuId}/sold-out")
  public ResponseEntity<BaseResponse<BoothMenuResponse>> updateMenuSoldOut(
      @AuthenticationPrincipal String departmentName,
      @PathVariable Long menuId,
      @Valid @RequestBody UpdateBoothMenuSoldOutRequest request) {
    BoothMenuResponse response =
        boothMenuService.updateMenuSoldOut(departmentName, menuId, request);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "메뉴 품절 여부 변경 성공", response));
  }

  @Operation(
      summary = "[ 총 관리자 | 토큰 O | 메뉴 삭제 ]",
      description =
          """
          **Parameters**  \n
          menuId: 삭제할 메뉴 식별자 \n
          \n
          **Returns**  \n
          메뉴 삭제 성공 여부 \n
          """)
  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/booth-menus/{menuId}")
  public ResponseEntity<BaseResponse<Void>> deleteMenu(@PathVariable Long menuId) {
    boothMenuService.deleteMenu(menuId);
    return ResponseEntity.status(200).body(BaseResponse.success(200, "메뉴 삭제 성공", null));
  }
}
