package com.skulikelion.festival.domain.lostitem.service;
import com.skulikelion.festival.domain.lostitem.dto.request.LostItemRequest;
import com.skulikelion.festival.domain.lostitem.dto.response.LostItemResponse;
import com.skulikelion.festival.domain.lostitem.entity.LostItem;
import com.skulikelion.festival.domain.lostitem.entity.SortType;
import com.skulikelion.festival.domain.lostitem.exception.LostItemErrorCode;
import com.skulikelion.festival.domain.lostitem.mapper.LostItemMapper;
import com.skulikelion.festival.domain.lostitem.repository.LostItemRepository;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.minio.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class LostItemService {

  private final LostItemRepository lostItemRepository;
  private final LostItemMapper lostItemMapper;
  private final MinioService minioService;

  /**
   * 분실물 생성 요청을 받아 저장 후 응답 DTO로 반환합니다.
   *
   * @param dto 분실물 생성 요청 DTO
   * @return 생성된 분실물 응답 DTO
   */
  @Transactional
  public LostItemResponse createLostItem(LostItemRequest dto, String imageUrl) {
    LostItem saved = lostItemRepository.save(lostItemMapper.toEntity(dto, imageUrl));

    log.info(
        "[분실물 생성 POST: /api/lost-items] 분실물 이름={},이미지 url={}, 위치={}, 날짜={}",
        saved.getName(),
        saved.getImageUrl(),
        saved.getFoundPlace(),
        saved.getFoundDate());

    return lostItemMapper.toDto(saved);
  }

  /**
   * 주어진 ID에 해당하는 분실물 정보를 조회합니다.
   *
   * @param id 조회할 분실물 ID
   * @return 조회된 분실물 응답 DTO
   */
  @Transactional(readOnly = true)
  public LostItemResponse findOne(Long id) {
    LostItem lostItem = getEntity(id);
    return lostItemMapper.toDto(lostItem);
  }

  /**
   * 검색어와 정렬 조건에 따라 분실물 목록을 조회합니다.
   *
   * @param name     검색할 분실물 이름 (선택)
   * @param sort     정렬 기준 (LATEST 또는 OLDEST)
   * @param pageable 페이지네이션 정보
   * @return 분실물 목록 페이지
   */
  @Transactional(readOnly = true)
  public Page<LostItemResponse> findAll(String name, SortType sort, Pageable pageable) {
    log.info(
        "[분실물 목록 조회 GET: /api/lost-items] 검색어: {}, 정렬: {}, 페이지 번호: {}, 페이지 크기: {}",
        name,
        sort,
        pageable.getPageNumber(),
        pageable.getPageSize());
    Page<LostItem> result;

    if (name == null || name.isBlank()) {
      result =
          (sort == SortType.LATEST)
              ? lostItemRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable)
              : lostItemRepository.findByIsDeletedFalseOrderByCreatedAtAsc(pageable);
    } else {
      result =
          (sort == SortType.LATEST)
              ? lostItemRepository
              .findByIsDeletedFalseAndNameContainingIgnoreCaseOrderByCreatedAtDesc(
                  name, pageable)
              : lostItemRepository
                  .findByIsDeletedFalseAndNameContainingIgnoreCaseOrderByCreatedAtAsc(
                      name, pageable);
    }

    return result.map(lostItemMapper::toDto);
  }

  /**
   * 주어진 ID의 분실물 정보를 수정하고 수정된 정보를 반환합니다.
   *
   * @param id  수정할 분실물 ID
   * @param dto 분실물 수정 요청 DTO
   * @return 수정된 분실물 응답 DTO
   */
  @Transactional
  public LostItemResponse update(Long id, LostItemRequest dto, String imageUrl) {
    LostItem lostItem = getEntity(id);

    String oldImageUrl = lostItem.getImageUrl();

    lostItemMapper.merge(lostItem, dto, imageUrl);

    if (imageUrl != null && oldImageUrl != null && !oldImageUrl.isBlank()) {
      deleteOldImage(oldImageUrl);
    }

    log.info(
        "[분실물 수정] ID={}, 이름={}, 위치={}, 날짜={}, 새이미지={}",
        id,
        lostItem.getName(),
        lostItem.getFoundPlace(),
        lostItem.getFoundDate(),
        lostItem.getImageUrl());

    return lostItemMapper.toDto(lostItem);
  }

  /**
   * 분실물의 수령 여부를 설정합니다.
   *
   * @param id       분실물 ID
   * @param returned 수령 여부 (true: 수령됨, false: 수령되지 않음)
   * @return 수령 여부가 변경된 분실물 응답 DTO
   */
  @Transactional
  public LostItemResponse setReturned(Long id, boolean returned) {
    LostItem lostItem = getEntity(id);

    if (returned && lostItem.isReturned()) {
      throw new CustomException(LostItemErrorCode.ITEM_ALREADY_RETURNED);
    }

    lostItem.setReturned(returned);
    lostItemRepository.save(lostItem);

    log.info("[수령 상태 변경 PATCH: /api/lost-items/{}] 수령 여부: {}", id, returned);

    return lostItemMapper.toDto(lostItem);
  }

  /**
   * 분실물을 삭제 처리합니다 (Soft Delete).
   *
   * @param id 삭제할 분실물 ID
   */
  @Transactional
  public void deleteById(Long id) {
    LostItem lostItem = getEntity(id);

    String oldImageUrl = lostItem.getImageUrl();
    if (oldImageUrl != null && !oldImageUrl.isBlank()) {
      deleteOldImage(oldImageUrl);
    }
    log.info("[분실물 삭제 DELETE: /api/lost-items/{}]", id);
    lostItem.setDeleted(true);
  }

  /**
   * 주어진 ID에 해당하는 삭제되지 않은 분실물을 조회합니다.
   *
   * @param id 조회할 분실물 ID
   * @return 삭제되지 않은 분실물 엔티티
   * @throws NoSuchElementException 분실물이 존재하지 않는 경우
   * @throws IllegalStateException  분실물이 삭제된 경우
   */
  private LostItem getEntity(Long id) {
    LostItem lostItem =
        lostItemRepository
            .findById(id)
            .orElseThrow(() -> new CustomException(LostItemErrorCode.ITEM_NOT_FOUND));

    if (lostItem.isDeleted()) {
      throw new CustomException(LostItemErrorCode.ITEM_ALREADY_DELETED);
    }
    log.info("[분실물 조회 내부 메서드] ID: {}", id);
    return lostItem;
  }

  /**
   * 기존 이미지 URL에서 파일 키를 추출하여 해당 이미지를 MinIO 버킷에서 삭제합니다.
   *
   * @param oldImageUrl 삭제할 이미지의 전체 URL
   */
  private void deleteOldImage(String oldImageUrl) {
    try {
      String key = extractKeyFromUrl(oldImageUrl);
      log.info("[이미지 삭제 시도] oldImageUrl={}, 추출된 key={}", oldImageUrl, key);
      minioService.deleteFile(key);
      log.info("[MinIO 삭제] 기존 파일 삭제 성공: {}", key);
    } catch (Exception e) {
      log.error("[MinIO 삭제 실패] 삭제 중 에러 발생", e);
    }
  }

  /**
   * MinIO 버킷 URL에서 파일 키를 추출합니다.
   *
   * @param imageUrl 전체 이미지 URL
   * @return 추출된 파일 키 (예: lostitem/파일명.jpg)
   * @throws IllegalArgumentException URL 형식이 잘못된 경우
   */
  private String extractKeyFromUrl(String imageUrl) {
    String bucketBaseUrl = "https://minio.2025skufestival.site/";
    if (imageUrl.startsWith(bucketBaseUrl)) {
      String fullKey = imageUrl.substring(bucketBaseUrl.length());
      int firstSlash = fullKey.indexOf("/");
      if (firstSlash != -1) {
        return fullKey.substring(firstSlash + 1);
      }
    }
    throw new IllegalArgumentException("URL 형식이 잘못되었습니다: " + imageUrl);
  }
}
