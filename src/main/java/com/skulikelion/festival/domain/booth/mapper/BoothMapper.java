package com.skulikelion.festival.domain.booth.mapper;

import com.skulikelion.festival.domain.booth.dto.response.*;
import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.entity.BoothImage;
import com.skulikelion.festival.domain.booth.entity.BoothMenu;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BoothMapper {

  /**
   * Booth Entity를 BoothResponse DTO로 변환
   */
  public BoothResponse toBoothResponse(Booth booth) {
    return BoothResponse.builder()
        .id(booth.getId())
        .name(booth.getName())
        .password(booth.getPassword())
        .waitingTeam(booth.getWaitingTeam())
        .openingHours(booth.getOpeningHours())
        .imageUrls(booth.getBoothImages().stream().map(BoothImage::getImageUrl).toList())
        .boothThumbnailUrl(booth.getBoothThumbnailUrl())
        .build();
  }

  /**
   * BoothMenu Entity를 BoothMenuResponse DTO로 변환
   */
  public BoothMenuResponse toBoothMenuResponse(BoothMenu boothMenu) {
    return BoothMenuResponse.builder()
        .menuKo(boothMenu.getMenuKo())
        .menuEn(boothMenu.getMenuEn())
        .menuCh(boothMenu.getMenuCh())
        .menuJp(boothMenu.getMenuJp())
        .menuPrice(boothMenu.getMenuPrice())
        .build();
  }

  /**
   * Booth Entity + booth faculty를 BothListResponse DTO로 변환
   */
  public static BoothListResponse toBoothListResponse(
      Booth booth, String faculty, String location, Boolean working) {
    return BoothListResponse.builder()
        .id(booth.getId())
        .boothFaculty(faculty)
        .boothLocation(location)
        .boothThumbnailUrl(booth.getBoothThumbnailUrl())
        .boothWaitings(booth.getWaitingTeam())
        .working(working)
        .serviceAgreement(booth.getServiceAgreement())
        .build();
  }

  /**
   * Booth Entity + faculty, title, description을 BoothDetailInfoResponse DTO로 변환
   */
  public static BoothDetailInfoResponse toBoothDetailInfoResponse(
      Booth booth,
      String faculty,
      String title,
      String description,
      List<InBoothMenuResponse> menuResponses) {
    return BoothDetailInfoResponse.builder()
        .id(booth.getId())
        .boothFaculty(faculty)
        .boothTitle(title)
        .boothDescription(description)
        .boothInstagram(booth.getBoothInstagram())
        .boothMenus(menuResponses)
        .openingHours(booth.getOpeningHours())
        .serviceAgreement(booth.getServiceAgreement())
        .imageUrls(booth.getBoothImages().stream().map(BoothImage::getImageUrl).toList())
        .build();
  }
}
