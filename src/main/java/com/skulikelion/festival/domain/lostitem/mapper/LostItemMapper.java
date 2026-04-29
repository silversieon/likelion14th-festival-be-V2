/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.mapper;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.lostitem.dto.response.LostItemListResponse;
import com.skulikelion.festival.domain.lostitem.dto.response.LostItemPageResponse;
import com.skulikelion.festival.domain.lostitem.dto.response.LostItemResponse;
import com.skulikelion.festival.domain.lostitem.entity.LostItem;

@Component
public class LostItemMapper {

  public LostItemResponse toResponse(LostItem lostItem, List<String> imageUrls) {
    return LostItemResponse.builder()
        .id(lostItem.getId())
        .name(lostItem.getName())
        .imageUrls(imageUrls)
        .foundPlace(lostItem.getFoundPlace())
        .foundDate(lostItem.getFoundDate())
        .dayOfWeek(getDay(lostItem.getFoundDate()))
        .isReturned(lostItem.isReturned())
        .build();
  }

  public LostItemListResponse toListResponse(LostItem lostItem) {
    return LostItemListResponse.builder()
        .id(lostItem.getId())
        .name(lostItem.getName())
        .imageUrl(lostItem.getImageUrl())
        .foundPlace(lostItem.getFoundPlace())
        .foundDate(lostItem.getFoundDate())
        .dayOfWeek(getDay(lostItem.getFoundDate()))
        .build();
  }

  public LostItemPageResponse toPageResponse(Page<LostItem> page) {
    return LostItemPageResponse.builder()
        .content(page.getContent().stream().map(this::toListResponse).toList())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .pageNum(page.getNumber() + 1)
        .pageSize(page.getSize())
        .last(page.isLast())
        .build();
  }

  private String getDay(LocalDate date) {
    return date.getDayOfWeek().toString();
  }
}
