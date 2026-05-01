/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.skulikelion.festival.domain.booth.dto.request.BoothMenuRequest;
import com.skulikelion.festival.domain.booth.dto.request.UpdateBoothMenuPriceRequest;
import com.skulikelion.festival.domain.booth.dto.request.UpdateBoothMenuSoldOutRequest;
import com.skulikelion.festival.domain.booth.dto.response.BoothMenuResponse;
import com.skulikelion.festival.domain.booth.dto.response.OrderAvailableBoothMenuGroupResponse;
import com.skulikelion.festival.global.enums.Language;

/**
 * 멋쟁이사자처럼 서경대학교 축제 페이지 부스 메뉴 관련 Service interface 입니다.
 *
 * @see com.skulikelion.festival.domain.booth.controller.BoothMenuController
 * @since 2026.05.01
 */
public interface BoothMenuService {

  /**
   * [ 부스 메뉴 생성 메서드 ]
   *
   * @param boothId 메뉴를 생성할 부스의 식별자
   * @param requests 부스 메뉴 생성 요청 정보 리스트
   * @param iconImages 메뉴 아이콘 이미지 리스트
   * @return 생성된 부스 메뉴 정보 리스트
   */
  List<BoothMenuResponse> createMenus(
      Long boothId, List<BoothMenuRequest> requests, List<MultipartFile> iconImages);

  /**
   * [ 부스 메뉴 수정 메서드 ]
   *
   * @param menuId 수정할 메뉴의 식별자
   * @param request 부스 메뉴 수정 요청 정보
   * @param iconImage 메뉴 아이콘 이미지
   * @return 수정된 부스 메뉴 정보
   */
  BoothMenuResponse updateMenu(Long menuId, BoothMenuRequest request, MultipartFile iconImage);

  /**
   * [ 부스 메뉴 가격 수정 메서드 ]
   *
   * @param departmentName 요청한 관리자의 학과명
   * @param menuId 가격을 수정할 메뉴의 식별자
   * @param request 부스 메뉴 가격 수정 요청 정보
   * @return 수정된 부스 메뉴 정보
   */
  BoothMenuResponse updateMenuPrice(
      String departmentName, Long menuId, UpdateBoothMenuPriceRequest request);

  /**
   * [ 부스 메뉴 품절 여부 변경 메서드 ]
   *
   * @param departmentName 요청한 관리자의 학과명
   * @param menuId 품절 여부를 변경할 메뉴의 식별자
   * @param request 부스 메뉴 품절 여부 변경 요청 정보
   * @return 변경된 부스 메뉴 정보
   */
  BoothMenuResponse updateMenuSoldOut(
      String departmentName, Long menuId, UpdateBoothMenuSoldOutRequest request);

  /**
   * [ 주문 가능 부스 메뉴 조회 메서드 ]
   *
   * @param boothId 메뉴를 조회할 부스의 식별자
   * @param language 조회할 메뉴명 언어
   * @return 카테고리별 주문 가능 메뉴 정보
   */
  OrderAvailableBoothMenuGroupResponse getOrderAvailableMenus(Long boothId, Language language);

  /**
   * [ 부스 메뉴 삭제 메서드 ]
   *
   * @param menuId 삭제할 메뉴의 식별자
   */
  void deleteMenu(Long menuId);
}
