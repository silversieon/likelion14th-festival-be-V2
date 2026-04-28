/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.skulikelion.festival.domain.booth.dto.request.BoothRequest;
import com.skulikelion.festival.domain.booth.dto.response.BoothAccountResponse;
import com.skulikelion.festival.domain.booth.dto.response.BoothListResponse;
import com.skulikelion.festival.domain.booth.dto.response.BoothResponse;
import com.skulikelion.festival.domain.booth.enums.BoothLocation;
import com.skulikelion.festival.global.enums.Language;

public interface BoothService {

  /**
   * [ 부스 생성 메서드 ]
   *
   * @param request 부스 생성 요청 정보
   * @param thumbnail 부스 썸네일 이미지
   * @param detailImages 부스 상세 이미지 리스트
   * @return 생성된 부스 정보
   */
  BoothResponse createBooth(
      BoothRequest request, MultipartFile thumbnail, List<MultipartFile> detailImages);

  /**
   * [ 부스 수정 메서드 ]
   *
   * @param boothId 수정할 부스의 식별자
   * @param request 부스 수정 요청 정보
   * @param thumbnail 부스 썸네일 이미지
   * @param detailImages 부스 상세 이미지 리스트
   * @return 수정된 부스 정보
   */
  BoothResponse updateBooth(
      Long boothId,
      BoothRequest request,
      MultipartFile thumbnail,
      List<MultipartFile> detailImages);

  /**
   * [ 부스 삭제 메서드 ]
   *
   * @param boothId 삭제할 부스의 식별자
   */
  void deleteBooth(Long boothId);

  /**
   * [ 위치별 부스 조회 메서드 ]
   *
   * @param location 조회할 부스 위치
   * @return 해당 위치의 부스 정보 리스트
   */
  List<BoothListResponse> getBoothsByLocation(BoothLocation location);

  /**
   * [ 부스 검색 메서드 ]
   *
   * @param keyword 검색할 학과명 키워드
   * @return 검색된 부스 정보 리스트
   */
  List<BoothListResponse> searchBooths(String keyword);

  /**
   * [ 부스 단건 조회 메서드 ]
   *
   * @param boothId 조회할 부스의 식별자
   * @param language 조회할 번역 언어
   * @return 부스 상세 정보
   */
  BoothResponse getBooth(Long boothId, Language language);

  /**
   * [ 입금 계좌 조회 메서드 ]
   *
   * @param boothId 조회할 부스의 식별자
   * @return 부스 입금 계좌 정보
   */
  BoothAccountResponse getBoothAccount(Long boothId);
}
