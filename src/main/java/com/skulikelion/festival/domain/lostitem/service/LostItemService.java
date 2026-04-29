/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.skulikelion.festival.domain.lostitem.dto.request.LostItemRequest;
import com.skulikelion.festival.domain.lostitem.dto.response.LostItemPageResponse;
import com.skulikelion.festival.domain.lostitem.dto.response.LostItemResponse;

public interface LostItemService {

  /**
   * [ 분실물 전체 조회 메서드 ]
   *
   * @param name 분실물 이름 검색어
   * @param foundDate 습득 날짜 필터
   * @param page 페이지 번호
   * @param size 페이지당 게시물 수
   * @return 분실물 페이지 응답
   */
  LostItemPageResponse getLostItems(String name, LocalDate foundDate, int page, int size);

  /**
   * [ 분실물 단건 조회 메서드 ]
   *
   * @param lostItemId 조회할 분실물의 식별자
   * @return 분실물 단건 응답
   */
  LostItemResponse getLostItem(Long lostItemId);

  /**
   * [ 분실물 등록 메서드 ]
   *
   * @param request 분실물 등록 요청 정보
   * @param images 분실물 이미지 리스트, 최소 1장 최대 4장
   * @return 등록된 분실물 응답
   */
  LostItemResponse createLostItem(LostItemRequest request, List<MultipartFile> images);

  /**
   * [ 분실물 수정 메서드 ]
   *
   * @param lostItemId 수정할 분실물의 식별자
   * @param request 분실물 수정 요청 정보, 기존 값 포함
   * @param images 분실물 이미지 리스트, 선택값. 전달 시 기존 이미지 전체 교체
   * @return 수정된 분실물 응답
   */
  LostItemResponse updateLostItem(
      Long lostItemId, LostItemRequest request, List<MultipartFile> images);

  /**
   * [ 분실물 삭제 메서드 ]
   *
   * @param lostItemId 삭제할 분실물의 식별자
   */
  void deleteLostItem(Long lostItemId);

  /**
   * [ 분실물 수령 상태 변경 메서드 ]
   *
   * @param lostItemId 상태를 변경할 분실물의 식별자
   * @param returned 수령 여부 (true: 수령 처리, false: 미수령 처리)
   * @return 상태가 변경된 분실물 응답
   */
  LostItemResponse updateLostItemStatus(Long lostItemId, boolean returned);
}
