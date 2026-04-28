/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.mapper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.booth.dto.request.BoothRequest;
import com.skulikelion.festival.domain.booth.dto.request.BoothTranslationRequest;
import com.skulikelion.festival.domain.booth.dto.response.BoothAccountResponse;
import com.skulikelion.festival.domain.booth.dto.response.BoothDetailImageResponse;
import com.skulikelion.festival.domain.booth.dto.response.BoothResponse;
import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.entity.BoothDetailImage;
import com.skulikelion.festival.domain.booth.entity.BoothTranslation;
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.global.exception.CustomException;

@Component
public class BoothMapper {

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  public Booth toBooth(
      BoothRequest request,
      String thumbnailUrl,
      LocalTime openTime,
      LocalTime orderOpenTime,
      LocalTime closeTime) {
    return Booth.builder()
        .department(request.getDepartment())
        .thumbnailUrl(thumbnailUrl)
        .openTime(openTime)
        .orderOpenTime(orderOpenTime)
        .closeTime(closeTime)
        .location(request.getLocation())
        .locationDetail(request.getLocationDetail())
        .accountName(request.getAccountName())
        .accountNumber(request.getAccountNumber())
        .bankName(request.getBankName())
        .build();
  }

  public BoothTranslation toBoothTranslation(Booth booth, BoothTranslationRequest request) {
    return BoothTranslation.builder()
        .booth(booth)
        .language(request.getLanguage())
        .departmentName(request.getDepartmentName())
        .boothName(request.getBoothName())
        .description(request.getDescription())
        .build();
  }

  public BoothDetailImage toBoothDetailImage(Booth booth, String imageUrl) {
    return BoothDetailImage.builder().booth(booth).imageUrl(imageUrl).build();
  }

  public BoothResponse toBoothResponse(
      Booth booth, BoothTranslation translation, List<BoothDetailImage> detailImages) {
    LocalTime now = LocalTime.now();
    return BoothResponse.builder()
        .boothId(booth.getId())
        .thumbnailUrl(booth.getThumbnailUrl())
        .open(booth.isOpen(now))
        .orderAvailable(booth.isOrderAvailable(now))
        .locationDetail(booth.getLocationDetail())
        .departmentName(translation.getDepartmentName())
        .boothName(translation.getBoothName())
        .description(translation.getDescription())
        .detailImages(toBoothDetailImageResponses(detailImages))
        .build();
  }

  public BoothAccountResponse toBoothAccountResponse(Booth booth) {
    return BoothAccountResponse.builder()
        .accountName(booth.getAccountName())
        .accountNumber(booth.getAccountNumber())
        .bankName(booth.getBankName())
        .build();
  }

  private List<BoothDetailImageResponse> toBoothDetailImageResponses(
      List<BoothDetailImage> detailImages) {
    return detailImages.stream()
        .map(
            detailImage ->
                BoothDetailImageResponse.builder().imageUrl(detailImage.getImageUrl()).build())
        .toList();
  }

  public LocalTime parseTime(String time) {
    if (time == null || time.isBlank()) {
      return null;
    }

    try {
      return LocalTime.parse(time, TIME_FORMATTER);
    } catch (DateTimeParseException e) {
      throw new CustomException(BoothErrorCode.INVALID_TIME_FORMAT);
    }
  }
}
