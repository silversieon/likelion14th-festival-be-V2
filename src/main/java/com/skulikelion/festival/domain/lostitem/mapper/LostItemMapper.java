/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.mapper;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.lostitem.dto.request.LostItemRequest;
import com.skulikelion.festival.domain.lostitem.dto.response.LostItemResponse;
import com.skulikelion.festival.domain.lostitem.entity.LostItem;

@Component
public class LostItemMapper {

  private LostItemMapper() {}

  public LostItemResponse toDto(LostItem lostItem) {
    return LostItemResponse.builder()
        .id(lostItem.getId())
        .name(lostItem.getName())
        .imageUrl(lostItem.getImageUrl())
        .foundPlace(lostItem.getFoundPlace())
        .foundDate(lostItem.getFoundDate())
        .isReturned(lostItem.isReturned())
        .createdAt(lostItem.getCreatedAt())
        .build();
  }

  public LostItem toEntity(LostItemRequest dto, String imageUrl) {
    return LostItem.builder()
        .name(dto.getName())
        .foundPlace(dto.getFoundPlace())
        .foundDate(dto.getFoundDate())
        .imageUrl(imageUrl)
        .isReturned(false)
        .isDeleted(false)
        .build();
  }

  public void merge(LostItem lostItem, LostItemRequest dto, String imageUrl) {
    if (dto.getName() != null) {
      lostItem.setName(dto.getName());
    }
    if (dto.getFoundPlace() != null) {
      lostItem.setFoundPlace(dto.getFoundPlace());
    }
    if (dto.getFoundDate() != null) {
      lostItem.setFoundDate(dto.getFoundDate());
    }
    if (imageUrl != null) {
      lostItem.setImageUrl(imageUrl);
    }
  }
}
