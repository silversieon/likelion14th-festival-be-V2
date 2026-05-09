/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.service.booth;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.skulikelion.festival.domain.booth.dto.request.booth.BoothOperationRequest;
import com.skulikelion.festival.domain.booth.dto.request.booth.BoothRequest;
import com.skulikelion.festival.domain.booth.dto.request.booth.BoothTranslationRequest;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothAccountResponse;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothListResponse;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothOperationResponse;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothResponse;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothThumbnailResponse;
import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.entity.BoothDetailImage;
import com.skulikelion.festival.domain.booth.entity.BoothMenu;
import com.skulikelion.festival.domain.booth.entity.BoothOperation;
import com.skulikelion.festival.domain.booth.entity.BoothTranslation;
import com.skulikelion.festival.domain.booth.enums.BoothLocation;
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.domain.booth.mapper.BoothMapper;
import com.skulikelion.festival.domain.booth.repository.BoothDetailImageRepository;
import com.skulikelion.festival.domain.booth.repository.BoothMenuRepository;
import com.skulikelion.festival.domain.booth.repository.BoothOperationRepository;
import com.skulikelion.festival.domain.booth.repository.BoothRepository;
import com.skulikelion.festival.domain.booth.repository.BoothTranslationRepository;
import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.domain.manager.exception.ManagerErrorCode;
import com.skulikelion.festival.domain.manager.repository.ManagerRepository;
import com.skulikelion.festival.global.enums.Department;
import com.skulikelion.festival.global.enums.Language;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.s3.enums.PathName;
import com.skulikelion.festival.global.s3.service.S3AsyncService;
import com.skulikelion.festival.global.s3.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoothServiceImpl implements BoothService {

  private static final int MAX_DETAIL_IMAGE_COUNT = 3;

  private final BoothRepository boothRepository;
  private final BoothTranslationRepository boothTranslationRepository;
  private final BoothDetailImageRepository boothDetailImageRepository;
  private final BoothOperationRepository boothOperationRepository;
  private final BoothMenuRepository boothMenuRepository;
  private final ManagerRepository managerRepository;
  private final BoothMapper boothMapper;
  private final S3Service s3Service;
  private final S3AsyncService s3AsyncService;

  @Override
  @Transactional
  public BoothResponse createBooth(
      BoothRequest request, MultipartFile thumbnail, List<MultipartFile> detailImages) {
    log.info("[BoothService] 부스 생성 요청 - 학과: {}", request.getDepartment());

    // 학과 중복 검증
    if (boothRepository.existsByDepartment(request.getDepartment())) {
      log.warn("[BoothService] 부스 생성 실패 - 중복 학과: {}", request.getDepartment());
      throw new CustomException(BoothErrorCode.BOOTH_ALREADY_EXISTS);
    }
    // 요청값 사전 검증
    validateTranslations(request.getTranslations());
    validateOperations(request.getOperations());
    validateDetailImageCount(detailImages);

    // 썸네일 업로드 및 부스 저장
    String thumbnailUrl = uploadImage(PathName.BOOTH_THUMBNAIL, thumbnail);
    log.info("[BoothService] 부스 썸네일 처리 완료 - thumbnailUrl: {}", thumbnailUrl);
    Booth booth = boothRepository.save(boothMapper.toBooth(request, thumbnailUrl));

    // 운영 정보 저장
    List<BoothOperation> operations =
        request.getOperations().stream()
            .map(operation -> boothMapper.toBoothOperation(booth, operation))
            .toList();
    boothOperationRepository.saveAll(operations);
    log.info(
        "[BoothService] 부스 운영 정보 저장 완료 - 부스 식별자: {}, 개수: {}", booth.getId(), operations.size());

    // 번역 정보 저장
    List<BoothTranslation> translations =
        request.getTranslations().stream()
            .map(translation -> boothMapper.toBoothTranslation(booth, translation))
            .toList();
    boothTranslationRepository.saveAll(translations);
    log.info(
        "[BoothService] 부스 번역 정보 저장 완료 - 부스 식별자: {}, 개수: {}", booth.getId(), translations.size());

    // 상세 이미지 저장
    List<BoothDetailImage> savedDetailImages = saveDetailImages(booth, detailImages);
    log.info(
        "[BoothService] 부스 상세 이미지 저장 완료 - 부스 식별자: {}, 개수: {}",
        booth.getId(),
        savedDetailImages.size());

    // 기본 응답 언어 선택
    BoothTranslation responseTranslation =
        translations.stream()
            .filter(translation -> translation.getLanguage() == Language.KO)
            .findFirst()
            .orElse(translations.getFirst());

    log.info("[BoothService] 부스 생성 발생 - 부스 식별자: {}, 학과: {}", booth.getId(), booth.getDepartment());
    return boothMapper.toBoothResponse(
        booth, responseTranslation, savedDetailImages, operations, List.of(), Language.KO);
  }

  @Override
  @Transactional
  public BoothResponse updateBooth(
      Long boothId,
      BoothRequest request,
      MultipartFile thumbnail,
      List<MultipartFile> detailImages) {
    log.info("[BoothService] 부스 수정 요청 - 부스 식별자: {}, 학과: {}", boothId, request.getDepartment());
    Booth booth = getBooth(boothId);

    // 학과 중복 검증
    if (!booth.getDepartment().equals(request.getDepartment())
        && boothRepository.existsByDepartment(request.getDepartment())) {
      log.warn("[BoothService] 부스 수정 실패 - 부스 식별자: {}, 중복 학과: {}", boothId, request.getDepartment());
      throw new CustomException(BoothErrorCode.BOOTH_ALREADY_EXISTS);
    }
    // 요청값 사전 검증
    validateTranslations(request.getTranslations());
    validateOperations(request.getOperations());
    validateDetailImageCount(detailImages);

    // 썸네일 전체 교체
    String oldThumbnailUrl = booth.getThumbnailUrl();
    String thumbnailUrl = null;
    if (thumbnail != null && !thumbnail.isEmpty()) {
      thumbnailUrl = uploadImage(PathName.BOOTH_THUMBNAIL, thumbnail);
    }
    log.info(
        "[BoothService] 부스 썸네일 전체 교체 완료 - 부스 식별자: {}, thumbnailUrl: {}", boothId, thumbnailUrl);

    // 부스 기본 정보 교체
    booth.update(
        request.getDepartment(),
        thumbnailUrl,
        request.getOrderEnabled(),
        request.getLocation(),
        request.getBoothNumbers(),
        request.getAccountName(),
        request.getAccountNumber(),
        request.getBankName());

    // 운영 정보 전체 교체
    boothOperationRepository.deleteByBoothId(boothId);
    boothOperationRepository.flush();
    List<BoothOperation> operations =
        request.getOperations().stream()
            .map(operation -> boothMapper.toBoothOperation(booth, operation))
            .toList();
    boothOperationRepository.saveAll(operations);
    log.info("[BoothService] 부스 운영 정보 전체 교체 완료 - 부스 식별자: {}, 개수: {}", boothId, operations.size());

    // 번역 정보 전체 교체
    boothTranslationRepository.deleteByBoothId(boothId);
    boothTranslationRepository.flush();
    List<BoothTranslation> translations =
        request.getTranslations().stream()
            .map(translation -> boothMapper.toBoothTranslation(booth, translation))
            .toList();
    boothTranslationRepository.saveAll(translations);
    log.info("[BoothService] 부스 번역 정보 전체 교체 완료 - 부스 식별자: {}, 개수: {}", boothId, translations.size());

    // 상세 이미지 전체 교체
    List<BoothDetailImage> oldDetailImages =
        boothDetailImageRepository.findByBoothIdOrderByIdAsc(boothId);
    boothDetailImageRepository.deleteByBoothId(boothId);
    boothDetailImageRepository.flush();
    List<BoothDetailImage> responseDetailImages = saveDetailImages(booth, detailImages);
    log.info(
        "[BoothService] 부스 상세 이미지 전체 교체 완료 - 부스 식별자: {}, 기존 개수: {}, 신규 개수: {}",
        boothId,
        oldDetailImages.size(),
        responseDetailImages.size());

    // 기본 응답 언어 선택
    BoothTranslation responseTranslation =
        translations.stream()
            .filter(translation -> translation.getLanguage() == Language.KO)
            .findFirst()
            .orElse(translations.getFirst());

    // DB 연관 데이터 교체 후 기존 이미지 정리
    deleteImageQuietly(oldThumbnailUrl);
    oldDetailImages.forEach(detailImage -> deleteImageQuietly(detailImage.getImageUrl()));

    log.info("[BoothService] 부스 수정 발생 - 부스 식별자: {}, 학과: {}", boothId, booth.getDepartment());
    List<BoothMenu> menus = boothMenuRepository.findByBoothIdOrderByIdAsc(boothId);
    return boothMapper.toBoothResponse(
        booth, responseTranslation, responseDetailImages, operations, menus, Language.KO);
  }

  @Override
  @Transactional
  public BoothThumbnailResponse updateBoothThumbnail(Long boothId, MultipartFile thumbnail) {
    log.info("[BoothService] 부스 썸네일 수정 요청 - 부스 식별자: {}", boothId);
    Booth booth = getBooth(boothId);
    String oldThumbnailUrl = booth.getThumbnailUrl();
    String thumbnailUrl = uploadRequiredImage(PathName.BOOTH_THUMBNAIL, thumbnail);

    booth.updateThumbnailUrl(thumbnailUrl);
    deleteImageQuietly(oldThumbnailUrl);

    log.info("[BoothService] 부스 썸네일 수정 발생 - 부스 식별자: {}, thumbnailUrl: {}", boothId, thumbnailUrl);
    return BoothThumbnailResponse.builder().boothId(boothId).thumbnailUrl(thumbnailUrl).build();
  }

  @Override
  @Transactional
  public BoothOperationResponse updateBoothOperation(
      String departmentName, Long boothId, BoothOperationRequest request) {
    log.info(
        "[BoothService] 부스 운영 시간 변경 요청 - 관리자 학과: {}, 부스 식별자: {}, 운영 날짜: {}",
        departmentName,
        boothId,
        request.getOperationDate());
    Booth booth = getBooth(boothId);

    // 부스 관리자 권한 검증
    validateBoothManager(departmentName, booth);

    // 운영 시간 요청값 검증
    validateOperationTime(request);
    LocalDate operationDate = boothMapper.parseDate(request.getOperationDate());
    BoothOperation operation =
        boothOperationRepository
            .findByBoothIdAndOperationDate(boothId, operationDate)
            .orElseThrow(
                () -> {
                  log.warn(
                      "[BoothService] 부스 운영 시간 변경 실패 - 운영 정보 없음, 부스 식별자: {}, 운영 날짜: {}",
                      boothId,
                      operationDate);
                  return new CustomException(BoothErrorCode.BOOTH_OPERATION_NOT_FOUND);
                });

    // 운영 시간 변경
    operation.update(
        request.getTimeType(),
        boothMapper.parseTime(request.getDayOpenTime()),
        boothMapper.parseTime(request.getNightOpenTime()),
        boothMapper.parseTime(request.getCloseTime()));
    log.info(
        "[BoothService] 부스 운영 시간 변경 발생 - 부스 식별자: {}, 운영 날짜: {}, 운영 타입: {}",
        boothId,
        operationDate,
        operation.getTimeType());
    return boothMapper.toBoothOperationResponse(operation);
  }

  @Override
  @Transactional(readOnly = true)
  public List<BoothOperationResponse> getBoothOperationInfos(String departmentName, Long boothId) {
    log.info("[BoothService] 부스 운영 시간 정보 조회 요청 - 관리자 학과: {}, 부스 식별자: {}", departmentName, boothId);
    Booth booth = getBooth(boothId);

    // 부스 관리자 권한 검증
    validateBoothManager(departmentName, booth);

    // 전체 운영 시간 조회
    List<BoothOperation> operations =
        boothOperationRepository.findByBoothIdOrderByOperationDateAsc(boothId);

    log.info(
        "[BoothService] 부스 운영 시간 정보 조회 발생 - 부스 식별자: {}, 운영 정보 개수: {}", boothId, operations.size());
    return operations.stream().map(boothMapper::toBoothOperationResponse).toList();
  }

  @Override
  @Transactional
  public void deleteBooth(Long boothId) {
    log.info("[BoothService] 부스 삭제 요청 - 부스 식별자: {}", boothId);
    Booth booth = getBooth(boothId);
    List<BoothDetailImage> detailImages =
        boothDetailImageRepository.findByBoothIdOrderByIdAsc(boothId);
    List<BoothMenu> menus = boothMenuRepository.findByBoothIdOrderByIdAsc(boothId);

    // S3 이미지 정리
    deleteImageQuietly(booth.getThumbnailUrl());
    detailImages.forEach(detailImage -> deleteImageQuietly(detailImage.getImageUrl()));
    menus.forEach(menu -> deleteImageQuietly(menu.getIconImageUrl()));

    // 부스 연관 데이터 삭제
    boothDetailImageRepository.deleteByBoothId(boothId);
    boothTranslationRepository.deleteByBoothId(boothId);
    boothOperationRepository.deleteByBoothId(boothId);
    boothMenuRepository.deleteByBoothId(boothId);
    boothRepository.delete(booth);
    log.info("[BoothService] 부스 연관 데이터 삭제 완료 - 부스 식별자: {}, 메뉴 개수: {}", boothId, menus.size());

    log.info("[BoothService] 부스 삭제 발생 - 부스 식별자: {}", boothId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<BoothListResponse> getBooths(BoothLocation location) {
    if (location == null) {
      List<BoothListResponse> responses = boothRepository.findBoothsByLanguage(Language.KO);
      log.info("[BoothService] 부스 전체 조회 발생 - 개수: {}", responses.size());
      return responses;
    }

    List<BoothListResponse> responses =
        boothRepository.findBoothsByLocationAndLanguage(location, Language.KO);
    log.info("[BoothService] 위치별 부스 조회 발생 - 위치: {}, 개수: {}", location, responses.size());
    return responses;
  }

  @Override
  @Transactional(readOnly = true)
  public List<BoothListResponse> searchBooths(String keyword) {
    List<BoothListResponse> responses =
        boothRepository.searchBoothsByDepartmentName(keyword, Language.KO);
    log.info("[BoothService] 부스 검색 발생 - 키워드: {}, 개수: {}", keyword, responses.size());
    return responses;
  }

  @Override
  @Transactional(readOnly = true)
  public BoothResponse getBooth(Long boothId, Language language) {
    log.info("[BoothService] 부스 단건 조회 요청 - 부스 식별자: {}, 언어: {}", boothId, language);
    Booth booth = getBooth(boothId);

    // 요청 언어 번역 조회
    BoothTranslation translation =
        boothTranslationRepository
            .findByBoothIdAndLanguage(boothId, language)
            .orElseThrow(
                () -> {
                  log.warn(
                      "[BoothService] 부스 단건 조회 실패 - 번역 정보 없음, 부스 식별자: {}, 언어: {}",
                      boothId,
                      language);
                  return new CustomException(BoothErrorCode.BOOTH_TRANSLATION_NOT_FOUND);
                });

    // 상세 이미지 순서 조회
    List<BoothDetailImage> detailImages =
        boothDetailImageRepository.findByBoothIdOrderByIdAsc(boothId);
    List<BoothOperation> operations =
        boothOperationRepository.findByBoothIdOrderByOperationDateAsc(boothId);
    List<BoothMenu> menus = boothMenuRepository.findByBoothIdOrderByIdAsc(boothId);
    log.info(
        "[BoothService] 부스 단건 조회 발생 - 부스 식별자: {}, 언어: {}, 상세 이미지 개수: {}, 운영 정보 개수: {}, 메뉴 개수: {}",
        boothId,
        language,
        detailImages.size(),
        operations.size(),
        menus.size());
    return boothMapper.toBoothResponse(
        booth, translation, detailImages, operations, menus, language);
  }

  @Override
  @Transactional(readOnly = true)
  public BoothAccountResponse getBoothAccount(Long boothId) {
    Booth booth = getBooth(boothId);
    log.info("[BoothService] 입금 계좌 조회 발생 - 부스 식별자: {}", boothId);
    return boothMapper.toBoothAccountResponse(booth);
  }

  private Booth getBooth(Long boothId) {
    return boothRepository
        .findById(boothId)
        .orElseThrow(
            () -> {
              log.warn("[BoothService] 부스 조회 실패 - 존재하지 않는 부스 식별자: {}", boothId);
              return new CustomException(BoothErrorCode.BOOTH_NOT_FOUND);
            });
  }

  private void validateBoothManager(String departmentName, Booth booth) {
    Department department = Department.valueOf(departmentName);
    Manager currentManager =
        managerRepository
            .findByDepartment(department)
            .orElseThrow(
                () -> {
                  log.warn("[BoothService] 매니저 조회 실패 - 학과명: {}", departmentName);
                  return new CustomException(ManagerErrorCode.MANAGER_NOT_FOUND);
                });

    if (!booth.getDepartment().equals(currentManager.getDepartment())
        && currentManager.getRole() != Role.ADMIN) {
      log.warn(
          "[BoothService] 부스 접근 권한 검증 실패 - 관리자 학과: {}, 부스 학과: {}",
          currentManager.getDepartment(),
          booth.getDepartment());
      throw new CustomException(BoothErrorCode.BOOTH_ACCESS_DENIED);
    }
  }

  private List<BoothDetailImage> saveDetailImages(Booth booth, List<MultipartFile> detailImages) {
    if (detailImages == null || detailImages.isEmpty()) {
      log.info("[BoothService] 부스 상세 이미지 없음 - 부스 식별자: {}", booth.getId());
      return List.of();
    }

    // 빈 파일 제외 후 업로드
    List<String> detailImageUrls = s3AsyncService.uploadFiles(PathName.BOOTH_DETAIL, detailImages);

    List<BoothDetailImage> boothDetailImages =
        detailImageUrls.stream()
            .map(imageUrl -> boothMapper.toBoothDetailImage(booth, imageUrl))
            .toList();
    log.info(
        "[BoothService] 부스 상세 이미지 업로드 완료 - 부스 식별자: {}, 개수: {}",
        booth.getId(),
        boothDetailImages.size());
    return boothDetailImageRepository.saveAll(boothDetailImages);
  }

  private String uploadImage(PathName pathName, MultipartFile image) {
    if (image == null || image.isEmpty()) {
      log.info("[BoothService] 이미지 업로드 생략 - 경로: {}", pathName);
      return null;
    }
    log.info("[BoothService] 이미지 업로드 요청 - 경로: {}, 파일명: {}", pathName, image.getOriginalFilename());
    return s3Service.uploadFile(pathName, image);
  }

  private String uploadRequiredImage(PathName pathName, MultipartFile image) {
    String fileName = image == null ? null : image.getOriginalFilename();
    log.info("[BoothService] 이미지 업로드 요청 - 경로: {}, 파일명: {}", pathName, fileName);
    return s3Service.uploadFile(pathName, image);
  }

  private void validateDetailImageCount(List<MultipartFile> detailImages) {
    if (detailImages == null) {
      return;
    }

    // 실제 파일 개수 검증
    long imageCount = detailImages.stream().filter(file -> file != null && !file.isEmpty()).count();
    if (imageCount > MAX_DETAIL_IMAGE_COUNT) {
      log.warn("[BoothService] 상세 이미지 개수 검증 실패 - 요청 개수: {}", imageCount);
      throw new CustomException(BoothErrorCode.BOOTH_DETAIL_IMAGE_LIMIT_EXCEEDED);
    }
    log.info("[BoothService] 상세 이미지 개수 검증 완료 - 요청 개수: {}", imageCount);
  }

  private void validateTranslations(List<BoothTranslationRequest> translations) {
    if (translations == null || translations.isEmpty()) {
      log.warn("[BoothService] 번역 정보 검증 실패 - 번역 정보 없음");
      throw new CustomException(BoothErrorCode.BOOTH_TRANSLATION_REQUIRED);
    }

    // 요청 언어 집합 추출
    Set<Language> requestLanguages =
        translations.stream().map(BoothTranslationRequest::getLanguage).collect(Collectors.toSet());

    // 언어 중복 검증
    if (requestLanguages.size() != translations.size()) {
      log.warn("[BoothService] 번역 정보 검증 실패 - 중복 언어 포함: {}", requestLanguages);
      throw new CustomException(BoothErrorCode.BOOTH_TRANSLATION_DUPLICATED);
    }

    // 전체 언어 포함 검증
    if (!requestLanguages.equals(EnumSet.allOf(Language.class))) {
      log.warn("[BoothService] 번역 정보 검증 실패 - 요청 언어: {}", requestLanguages);
      throw new CustomException(BoothErrorCode.BOOTH_TRANSLATION_REQUIRED);
    }
    log.info("[BoothService] 번역 정보 검증 완료 - 요청 언어: {}", requestLanguages);
  }

  private void validateOperations(List<BoothOperationRequest> operations) {
    if (operations == null || operations.isEmpty()) {
      log.warn(
          "[BoothService] 운영 정보 검증 실패 - 요청 개수: {}", operations == null ? 0 : operations.size());
      throw new CustomException(BoothErrorCode.BOOTH_OPERATION_REQUIRED);
    }

    Set<LocalDate> operationDates =
        operations.stream()
            .map(operation -> boothMapper.parseDate(operation.getOperationDate()))
            .collect(Collectors.toSet());
    if (operationDates.size() != operations.size()) {
      log.warn("[BoothService] 운영 정보 검증 실패 - 중복 날짜 포함: {}", operationDates);
      throw new CustomException(BoothErrorCode.BOOTH_OPERATION_DUPLICATED);
    }

    operations.forEach(this::validateOperationTime);
    log.info("[BoothService] 운영 정보 검증 완료 - 운영 날짜: {}", operationDates);
  }

  private void validateOperationTime(BoothOperationRequest operation) {
    LocalTime dayOpenTime = boothMapper.parseTime(operation.getDayOpenTime());
    LocalTime nightOpenTime = boothMapper.parseTime(operation.getNightOpenTime());
    LocalTime closeTime = boothMapper.parseTime(operation.getCloseTime());

    if (operation.getTimeType() == null || closeTime == null) {
      throw new CustomException(BoothErrorCode.BOOTH_OPERATION_TIME_REQUIRED);
    }

    switch (operation.getTimeType()) {
      case DAY -> {
        if (dayOpenTime == null) {
          throw new CustomException(BoothErrorCode.BOOTH_OPERATION_TIME_REQUIRED);
        }
      }
      case NIGHT -> {
        if (nightOpenTime == null) {
          throw new CustomException(BoothErrorCode.BOOTH_OPERATION_TIME_REQUIRED);
        }
      }
      case ALL -> {
        if (dayOpenTime == null || nightOpenTime == null) {
          throw new CustomException(BoothErrorCode.BOOTH_OPERATION_TIME_REQUIRED);
        }
      }
    }
  }

  private void deleteImageQuietly(String imageUrl) {
    if (imageUrl == null || imageUrl.isBlank()) {
      return;
    }
    try {
      // S3 이미지 삭제
      String keyName = s3Service.extractKeyNameFromUrl(imageUrl);
      s3Service.deleteFile(keyName);
      log.info("[BoothService] S3 이미지 삭제 완료 - imageUrl: {}", imageUrl);
    } catch (RuntimeException e) {
      log.warn("[BoothService] S3 이미지 삭제 실패 - imageUrl: {}", imageUrl, e);
    }
  }
}
