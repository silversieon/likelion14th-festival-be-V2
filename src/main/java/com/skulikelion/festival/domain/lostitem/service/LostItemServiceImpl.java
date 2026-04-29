/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.skulikelion.festival.domain.lostitem.dto.request.LostItemRequest;
import com.skulikelion.festival.domain.lostitem.dto.response.LostItemPageResponse;
import com.skulikelion.festival.domain.lostitem.dto.response.LostItemResponse;
import com.skulikelion.festival.domain.lostitem.entity.LostItem;
import com.skulikelion.festival.domain.lostitem.entity.LostItemImage;
import com.skulikelion.festival.domain.lostitem.exception.LostItemErrorCode;
import com.skulikelion.festival.domain.lostitem.mapper.LostItemMapper;
import com.skulikelion.festival.domain.lostitem.repository.LostItemImageRepository;
import com.skulikelion.festival.domain.lostitem.repository.LostItemRepository;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.s3.enums.PathName;
import com.skulikelion.festival.global.s3.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LostItemServiceImpl implements LostItemService {

  private static final int MIN_LOST_ITEM_IMAGE_COUNT = 1;
  private static final int MAX_LOST_ITEM_IMAGE_COUNT = 4;

  private final LostItemRepository lostItemRepository;
  private final LostItemMapper lostItemMapper;
  private final S3Service s3Service;
  private final LostItemImageRepository lostItemImageRepository;

  @Override
  @Transactional(readOnly = true)
  public LostItemPageResponse getLostItems(String name, LocalDate foundDate, int page, int size) {

    log.info(
        "[LostItemService] 분실물 전체 조회 요청 - 이름: {}, 습득 날짜: {}, 페이지: {}, 크기: {}",
        name,
        foundDate,
        page,
        size);

    // 페이지 번호 검증
    if (page < 0) {
      log.warn("[LostItemService] 분실물 전체 조회 실패 - 잘못된 페이지 번호: {}", page);
      throw new CustomException(LostItemErrorCode.INVALID_PAGE_REQUEST);
    }

    // 페이지 크기 검증
    if (size <= 0) {
      log.warn("[LostItemService] 분실물 전체 조회 실패 - 잘못된 페이지 크기: {}", size);
      throw new CustomException(LostItemErrorCode.INVALID_PAGE_SIZE_REQUEST);
    }

    Pageable pageable = PageRequest.of(page, size);

    boolean hasName = name != null && !name.isBlank();
    boolean hasDate = foundDate != null;

    Page<LostItem> lostItems;

    // 이름 + 날짜 조건 조회
    if (hasName && hasDate) {
      lostItems =
          lostItemRepository.findByNameContainingIgnoreCaseAndFoundDateOrderByCreatedAtDesc(
              name, foundDate, pageable);

      // 이름 조건 조회
    } else if (hasName) {
      lostItems =
          lostItemRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc(name, pageable);

      // 날짜 조건 조회
    } else if (hasDate) {
      lostItems = lostItemRepository.findByFoundDateOrderByCreatedAtDesc(foundDate, pageable);

      // 전체 조회
    } else {
      lostItems = lostItemRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    log.info(
        "[LostItemService] 분실물 전체 조회 성공 - 조회 개수: {}, 전체 개수: {}",
        lostItems.getNumberOfElements(),
        lostItems.getTotalElements());

    return lostItemMapper.toPageResponse(lostItems);
  }

  @Override
  @Transactional(readOnly = true)
  public LostItemResponse getLostItem(Long lostItemId) {

    LostItem lostItem = getLostItemEntity(lostItemId);

    List<String> imageUrls =
        lostItemImageRepository.findByLostItemIdOrderByIdAsc(lostItemId).stream()
            .map(LostItemImage::getImageUrl)
            .toList();

    return lostItemMapper.toResponse(lostItem, imageUrls);
  }

  private LostItem getLostItemEntity(Long lostItemId) {
    return lostItemRepository
        .findById(lostItemId)
        .orElseThrow(
            () -> {
              log.warn("[LostItemService] 분실물 조회 실패 - 존재하지 않는 분실물 식별자: {}", lostItemId);
              return new CustomException(LostItemErrorCode.ITEM_NOT_FOUND);
            });
  }

  @Override
  @Transactional
  public LostItemResponse createLostItem(LostItemRequest request, List<MultipartFile> images) {
    log.info("[LostItemService] 분실물 등록 요청 - 이름: {}", request.getName());

    // 습득 날짜 검증
    validateFoundDate(request.getFoundDate());

    // 이미지 개수 검증
    validateImageCount(images);

    // 이미지 업로드
    List<String> imageUrls =
        images.stream()
            .filter(image -> image != null && !image.isEmpty())
            .map(image -> uploadImage(PathName.LOST_ITEM, image))
            .toList();

    // 분실물 저장
    LostItem lostItem =
        lostItemRepository.save(
            LostItem.builder()
                .name(request.getName())
                .imageUrl(imageUrls.get(0))
                .foundPlace(request.getFoundPlace())
                .foundDate(request.getFoundDate())
                .isReturned(false)
                .build());

    // 분실물 이미지 저장
    List<LostItemImage> lostItemImages =
        imageUrls.stream()
            .map(imageUrl -> LostItemImage.builder().lostItem(lostItem).imageUrl(imageUrl).build())
            .toList();

    lostItemImageRepository.saveAll(lostItemImages);

    log.info("[LostItemService] 분실물 등록 성공 - 분실물 식별자: {}", lostItem.getId());

    return lostItemMapper.toResponse(lostItem, imageUrls);
  }

  private String uploadImage(PathName pathName, MultipartFile image) {
    log.info(
        "[LostItemService] 이미지 업로드 요청 - 경로: {}, 파일명: {}", pathName, image.getOriginalFilename());

    return s3Service.uploadFile(pathName, image);
  }

  private void validateImageCount(List<MultipartFile> images) {
    if (images == null || images.isEmpty()) {
      log.warn("[LostItemService] 분실물 이미지 검증 실패 - 이미지 없음");
      throw new CustomException(LostItemErrorCode.LOST_ITEM_IMAGE_REQUIRED);
    }

    long imageCount = images.stream().filter(image -> image != null && !image.isEmpty()).count();

    if (imageCount < MIN_LOST_ITEM_IMAGE_COUNT || imageCount > MAX_LOST_ITEM_IMAGE_COUNT) {
      log.warn("[LostItemService] 분실물 이미지 개수 검증 실패 - 요청 개수: {}", imageCount);
      throw new CustomException(LostItemErrorCode.LOST_ITEM_IMAGE_COUNT_INVALID);
    }

    log.info("[LostItemService] 분실물 이미지 개수 검증 완료 - 요청 개수: {}", imageCount);
  }

  private void validateFoundDate(LocalDate foundDate) {

    if (foundDate == null) {
      throw new CustomException(LostItemErrorCode.INVALID_DATE_FORMAT);
    }

    LocalDate start = LocalDate.of(2026, 5, 13);
    LocalDate end = LocalDate.of(2026, 5, 15);

    if (foundDate.isBefore(start) || foundDate.isAfter(end)) {
      log.warn("[LostItemService] 잘못된 날짜 요청 - {}", foundDate);
      throw new CustomException(LostItemErrorCode.INVALID_FESTIVAL_DATE);
    }
  }

  @Override
  @Transactional
  public LostItemResponse updateLostItem(
      Long lostItemId, LostItemRequest request, List<MultipartFile> images) {

    log.info("[LostItemService] 분실물 수정 요청 - 분실물 식별자: {}", lostItemId);

    LostItem lostItem = getLostItemEntity(lostItemId);

    // 습득 날짜 검증
    validateFoundDate(request.getFoundDate());

    // 분실물 기본 정보 수정
    lostItem.setName(request.getName());
    lostItem.setFoundPlace(request.getFoundPlace());
    lostItem.setFoundDate(request.getFoundDate());

    List<String> imageUrls;

    // 이미지가 전달된 경우에만 기존 이미지 전체 교체
    if (hasImages(images)) {
      validateImageCount(images);

      List<LostItemImage> oldImages =
          lostItemImageRepository.findByLostItemIdOrderByIdAsc(lostItemId);

      List<String> newImageUrls =
          images.stream()
              .filter(image -> image != null && !image.isEmpty())
              .map(image -> uploadImage(PathName.LOST_ITEM, image))
              .toList();

      lostItemImageRepository.deleteByLostItemId(lostItemId);

      List<LostItemImage> newLostItemImages =
          newImageUrls.stream()
              .map(
                  imageUrl -> LostItemImage.builder().lostItem(lostItem).imageUrl(imageUrl).build())
              .toList();

      lostItemImageRepository.saveAll(newLostItemImages);

      lostItem.setImageUrl(newImageUrls.get(0));

      deleteImagesFromS3(oldImages);

      imageUrls = newImageUrls;

    } else {
      // 이미지가 전달되지 않은 경우 기존 이미지 유지
      imageUrls =
          lostItemImageRepository.findByLostItemIdOrderByIdAsc(lostItemId).stream()
              .map(LostItemImage::getImageUrl)
              .toList();
    }

    log.info("[LostItemService] 분실물 수정 성공 - 분실물 식별자: {}", lostItemId);

    return lostItemMapper.toResponse(lostItem, imageUrls);
  }

  @Override
  @Transactional
  public void deleteLostItem(Long lostItemId) {

    log.info("[LostItemService] 분실물 삭제 요청 - 분실물 식별자: {}", lostItemId);

    LostItem lostItem = getLostItemEntity(lostItemId);

    // 기존 이미지 조회
    List<LostItemImage> images = lostItemImageRepository.findByLostItemIdOrderByIdAsc(lostItemId);

    // S3 이미지 삭제
    deleteImagesFromS3(images);

    // 분실물 이미지 DB 삭제
    lostItemImageRepository.deleteByLostItemId(lostItemId);

    // 분실물 DB 삭제
    lostItemRepository.delete(lostItem);

    log.info("[LostItemService] 분실물 삭제 성공 - 분실물 식별자: {}", lostItemId);
  }

  private boolean hasImages(List<MultipartFile> images) {
    return images != null && images.stream().anyMatch(image -> image != null && !image.isEmpty());
  }

  private void deleteImagesFromS3(List<LostItemImage> images) {
    images.forEach(
        image -> {
          String keyName = s3Service.extractKeyNameFromUrl(image.getImageUrl());
          s3Service.deleteFile(keyName);
        });
  }

  @Override
  @Transactional
  public LostItemResponse updateLostItemStatus(Long lostItemId, boolean returned) {

    log.info("[LostItemService] 분실물 수령 상태 변경 요청 - id: {}, 요청 상태: {}", lostItemId, returned);

    LostItem lostItem = getLostItemEntity(lostItemId);

    lostItem.setReturned(returned);

    List<String> imageUrls =
        lostItemImageRepository.findByLostItemIdOrderByIdAsc(lostItemId).stream()
            .map(LostItemImage::getImageUrl)
            .toList();

    log.info("[LostItemService] 분실물 수령 상태 변경 성공 - id: {}, 변경 상태: {}", lostItemId, returned);

    return lostItemMapper.toResponse(lostItem, imageUrls);
  }
}
