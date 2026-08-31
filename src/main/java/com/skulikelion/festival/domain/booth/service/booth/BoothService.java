/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.service.booth;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.skulikelion.festival.domain.booth.dto.request.booth.BoothOperationRequest;
import com.skulikelion.festival.domain.booth.dto.request.booth.BoothRequest;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothAccountResponse;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothBusinessInfoResponse;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothListResponse;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothOperationResponse;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothResponse;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothThumbnailResponse;
import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.enums.BoothLocation;
import com.skulikelion.festival.domain.booth.enums.BoothStatus;
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
   * [ 부스 썸네일 수정 메서드 ]
   *
   * @param boothId 썸네일을 수정할 부스의 식별자
   * @param thumbnail 부스 썸네일 이미지
   * @return 수정된 부스 썸네일 정보
   */
  BoothThumbnailResponse updateBoothThumbnail(Long boothId, MultipartFile thumbnail);

  /**
   * [ 부스 운영 시간 변경 메서드 ]
   *
   * @param departmentName 요청한 관리자의 학과명
   * @param boothId 운영 시간을 변경할 부스의 식별자
   * @param request 부스 운영 시간 변경 요청 정보
   * @return 변경된 부스 운영 정보
   */
  BoothOperationResponse updateBoothOperation(
      String departmentName, Long boothId, BoothOperationRequest request);

  /**
   * [ 부스 운영 시간 정보 조회 메서드 ]
   *
   * @param departmentName 요청한 관리자의 학과명
   * @param boothId 운영 시간 정보를 조회할 부스의 식별자
   * @return 부스 운영 시간 정보 리스트
   */
  List<BoothOperationResponse> getBoothOperationInfos(String departmentName, Long boothId);

  /**
   * [ 부스 삭제 메서드 ]
   *
   * @param boothId 삭제할 부스의 식별자
   */
  void deleteBooth(Long boothId);

  /**
   * [ 부스 목록 조회 메서드 ]
   *
   * @param location 조회할 부스 위치
   * @return 부스 정보 리스트
   */
  List<BoothListResponse> getBooths(BoothLocation location);

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

  /**
   * [ 부스 영업 정보 조회 메서드 ]
   *
   * @param departmentName 학과명
   * @param date 조회할 날짜
   * @return 부스 영업 정보
   */
  BoothBusinessInfoResponse getBoothBusinessInfo(String departmentName, LocalDate date);

  /**
   * [ 부스 영업 중 전환 메서드 ]
   *
   * @param departmentName 학과명
   */
  void changeBoothStatusToOpen(String departmentName);

  /**
   * [ 부스 영업 중단 전환 메서드 ]
   *
   * @param departmentName 학과명
   * @param boothStatus 부스 상태
   */
  void changeBoothStatusToClose(String departmentName, BoothStatus boothStatus);

  /** [ 모든 부스 상태 변경 메서드 ] 영업 종료로 변경 */
  void allBoothStatusToClose();

  /** [ 오픈 시간인 부스 상태 변경 메서드 ] 영업 중으로 변경 */
  void updateBoothStatusToOpen(LocalDate today, LocalTime now);

  /** [ 마감 시간인 부스 상태 변경 메서드 ] 영업 종료로 변경 */
  void updateBoothStatusToClose(LocalDate today, LocalTime now);

  /**
   * [ 부스 정보 조회 메서드 ] 필요한 학과명으로 부스 조회
   *
   * @param departmentName 학과명
   * @return 부스 엔티티
   */
  Booth getRequiredBooth(String departmentName);
}
