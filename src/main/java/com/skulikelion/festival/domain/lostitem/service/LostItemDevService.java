/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.lostitem.dto.response.LostItemResponse;
import com.skulikelion.festival.domain.lostitem.entity.LostItem;
import com.skulikelion.festival.domain.lostitem.exception.LostItemErrorCode;
import com.skulikelion.festival.domain.lostitem.mapper.LostItemMapper;
import com.skulikelion.festival.domain.lostitem.repository.LostItemRepository;
import com.skulikelion.festival.global.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LostItemDevService {

  private final LostItemRepository lostItemRepository;
  private final LostItemMapper lostItemMapper;

  /**
   * 삭제된 분실물 목록을 조회합니다.
   *
   * @return 삭제된 분실물 응답 DTO 리스트
   */
  @Transactional(readOnly = true)
  public List<LostItemResponse> findDeletedItems() {
    log.info("[개발자]삭제된 분실물 조회");
    return lostItemRepository.findByIsDeletedTrueOrderByCreatedAtDesc().stream()
        .map(lostItemMapper::toDto)
        .toList();
  }

  /**
   * 삭제된 분실물을 복구합니다.
   *
   * @param id 복구할 분실물 ID
   * @return 복구된 분실물 응답 DTO
   */
  @Transactional
  public LostItemResponse restore(Long id) {
    LostItem lostItem =
        lostItemRepository
            .findById(id)
            .orElseThrow(() -> new CustomException(LostItemErrorCode.ITEM_NOT_FOUND));

    if (!lostItem.isDeleted()) {
      throw new CustomException(LostItemErrorCode.ITEM_ALREADY_RESTORED);
    }

    lostItem.setDeleted(false);

    log.info("[분실물 복구 PUT: /api/dev/lost-items/{}/restore] 복구 완료", id);
    return lostItemMapper.toDto(lostItem);
  }
}
