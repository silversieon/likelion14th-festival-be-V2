/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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
import com.skulikelion.festival.global.s3.service.S3AsyncService;
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
  private final S3AsyncService s3AsyncService;
  private final TransactionTemplate transactionTemplate;

  @Override
  @Transactional(readOnly = true)
  public LostItemPageResponse getLostItems(String name, LocalDate foundDate, int page, int size) {
    log.info(
        "[LostItemService] 분실물 전체 조회 요청 - 이름: {}, 습득 날짜: {}, 페이지: {}, 크기: {}",
        name,
        foundDate,
        page,
        size);

    if (page < 0) {
      throw new CustomException(LostItemErrorCode.INVALID_PAGE_REQUEST);
    }

    if (size <= 0) {
      throw new CustomException(LostItemErrorCode.INVALID_PAGE_SIZE_REQUEST);
    }

    Pageable pageable = PageRequest.of(page, size);

    boolean hasName = name != null && !name.isBlank();
    boolean hasDate = foundDate != null;

    Page<LostItem> lostItems;

    if (hasName && hasDate) {
      lostItems =
          lostItemRepository
              .findByNameContainingIgnoreCaseAndFoundDateOrderByIsReturnedAscCreatedAtDesc(
                  name, foundDate, pageable);
    } else if (hasName) {
      lostItems =
          lostItemRepository.findByNameContainingIgnoreCaseOrderByIsReturnedAscCreatedAtDesc(
              name, pageable);
    } else if (hasDate) {
      lostItems =
          lostItemRepository.findByFoundDateOrderByIsReturnedAscCreatedAtDesc(foundDate, pageable);
    } else {
      lostItems = lostItemRepository.findAllByOrderByIsReturnedAscCreatedAtDesc(pageable);
    }

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

  @Override
  public LostItemResponse createLostItem(LostItemRequest request, List<MultipartFile> images) {
    log.info("[LostItemService] 분실물 등록 요청 - 이름: {}", request.getName());

    validateFoundDate(request.getFoundDate());
    validateImageCount(images);

    List<String> imageUrls = s3AsyncService.uploadFiles(PathName.LOST_ITEM, images);

    try {
      return Objects.requireNonNull(
          transactionTemplate.execute(
              status -> {
                LostItem lostItem =
                    lostItemRepository.save(
                        LostItem.builder()
                            .name(request.getName())
                            .imageUrl(imageUrls.get(0))
                            .foundPlace(request.getFoundPlace())
                            .foundDate(request.getFoundDate())
                            .isReturned(false)
                            .build());

                List<LostItemImage> lostItemImages =
                    imageUrls.stream()
                        .map(
                            imageUrl ->
                                LostItemImage.builder()
                                    .lostItem(lostItem)
                                    .imageUrl(imageUrl)
                                    .build())
                        .toList();

                lostItemImageRepository.saveAll(lostItemImages);

                log.info("[LostItemService] 분실물 등록 성공 - 분실물 식별자: {}", lostItem.getId());

                return lostItemMapper.toResponse(lostItem, imageUrls);
              }));
    } catch (RuntimeException e) {
      deleteImageUrlsQuietly(imageUrls);
      throw e;
    }
  }

  @Override
  public LostItemResponse updateLostItem(
      Long lostItemId, LostItemRequest request, List<MultipartFile> images) {
    log.info("[LostItemService] 분실물 수정 요청 - 분실물 식별자: {}", lostItemId);

    validateFoundDate(request.getFoundDate());

    if (!hasImages(images)) {
      return Objects.requireNonNull(
          transactionTemplate.execute(
              status -> {
                LostItem lostItem = getLostItemEntity(lostItemId);

                lostItem.setName(request.getName());
                lostItem.setFoundPlace(request.getFoundPlace());
                lostItem.setFoundDate(request.getFoundDate());

                List<String> imageUrls =
                    lostItemImageRepository.findByLostItemIdOrderByIdAsc(lostItemId).stream()
                        .map(LostItemImage::getImageUrl)
                        .toList();

                return lostItemMapper.toResponse(lostItem, imageUrls);
              }));
    }

    validateImageCount(images);

    List<String> newImageUrls = s3AsyncService.uploadFiles(PathName.LOST_ITEM, images);

    try {
      UpdateLostItemResult result =
          Objects.requireNonNull(
              transactionTemplate.execute(
                  status -> {
                    LostItem lostItem = getLostItemEntity(lostItemId);

                    List<LostItemImage> oldImages =
                        lostItemImageRepository.findByLostItemIdOrderByIdAsc(lostItemId);

                    List<String> oldImageUrls =
                        oldImages.stream().map(LostItemImage::getImageUrl).toList();

                    lostItem.setName(request.getName());
                    lostItem.setFoundPlace(request.getFoundPlace());
                    lostItem.setFoundDate(request.getFoundDate());
                    lostItem.setImageUrl(newImageUrls.get(0));

                    lostItemImageRepository.deleteByLostItemId(lostItemId);

                    List<LostItemImage> newLostItemImages =
                        newImageUrls.stream()
                            .map(
                                imageUrl ->
                                    LostItemImage.builder()
                                        .lostItem(lostItem)
                                        .imageUrl(imageUrl)
                                        .build())
                            .toList();

                    lostItemImageRepository.saveAll(newLostItemImages);

                    return new UpdateLostItemResult(
                        lostItemMapper.toResponse(lostItem, newImageUrls), oldImageUrls);
                  }));

      deleteImageUrlsQuietly(result.oldImageUrls());

      log.info("[LostItemService] 분실물 수정 성공 - 분실물 식별자: {}", lostItemId);

      return result.response();

    } catch (RuntimeException e) {
      deleteImageUrlsQuietly(newImageUrls);
      throw e;
    }
  }

  @Override
  @Transactional
  public void deleteLostItem(Long lostItemId) {
    log.info("[LostItemService] 분실물 삭제 요청 - 분실물 식별자: {}", lostItemId);

    LostItem lostItem = getLostItemEntity(lostItemId);

    List<LostItemImage> images = lostItemImageRepository.findByLostItemIdOrderByIdAsc(lostItemId);

    deleteImagesFromS3(images);

    lostItemImageRepository.deleteByLostItemId(lostItemId);
    lostItemRepository.delete(lostItem);

    log.info("[LostItemService] 분실물 삭제 성공 - 분실물 식별자: {}", lostItemId);
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

  private void deleteImageUrlsQuietly(List<String> imageUrls) {
    if (imageUrls == null || imageUrls.isEmpty()) {
      return;
    }

    imageUrls.forEach(
        imageUrl -> {
          try {
            String keyName = s3Service.extractKeyNameFromUrl(imageUrl);
            s3Service.deleteFile(keyName);
          } catch (RuntimeException e) {
            log.warn("[LostItemService] S3 이미지 정리 실패 - imageUrl: {}", imageUrl, e);
          }
        });
  }

  private record UpdateLostItemResult(LostItemResponse response, List<String> oldImageUrls) {}
}
