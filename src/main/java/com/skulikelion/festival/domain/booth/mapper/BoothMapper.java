/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.mapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.booth.dto.request.booth.BoothOperationRequest;
import com.skulikelion.festival.domain.booth.dto.request.booth.BoothRequest;
import com.skulikelion.festival.domain.booth.dto.request.booth.BoothTranslationRequest;
import com.skulikelion.festival.domain.booth.dto.request.menu.BoothMenuRequest;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothAccountResponse;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothDetailImageResponse;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothOperationResponse;
import com.skulikelion.festival.domain.booth.dto.response.booth.BoothResponse;
import com.skulikelion.festival.domain.booth.dto.response.menu.BoothMenuResponse;
import com.skulikelion.festival.domain.booth.dto.response.menu.BoothMenuSummaryGroupResponse;
import com.skulikelion.festival.domain.booth.dto.response.menu.BoothMenuSummaryResponse;
import com.skulikelion.festival.domain.booth.dto.response.menu.OrderAvailableBoothMenuGroupResponse;
import com.skulikelion.festival.domain.booth.dto.response.menu.OrderAvailableBoothMenuResponse;
import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.entity.BoothDetailImage;
import com.skulikelion.festival.domain.booth.entity.BoothMenu;
import com.skulikelion.festival.domain.booth.entity.BoothOperation;
import com.skulikelion.festival.domain.booth.entity.BoothTranslation;
import com.skulikelion.festival.domain.booth.enums.MenuCategory;
import com.skulikelion.festival.domain.booth.enums.TimeType;
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.global.enums.Language;
import com.skulikelion.festival.global.exception.CustomException;

@Component
public class BoothMapper {

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

  public Booth toBooth(BoothRequest request, String thumbnailUrl) {
    return Booth.builder()
        .department(request.getDepartment())
        .thumbnailUrl(thumbnailUrl)
        .orderEnabled(Boolean.TRUE.equals(request.getOrderEnabled()))
        .location(request.getLocation())
        .boothNumbers(request.getBoothNumbers())
        .accountName(request.getAccountName())
        .accountNumber(request.getAccountNumber())
        .bankName(request.getBankName())
        .build();
  }

  public BoothOperation toBoothOperation(Booth booth, BoothOperationRequest request) {
    return BoothOperation.builder()
        .booth(booth)
        .operationDate(parseDate(request.getOperationDate()))
        .timeType(request.getTimeType())
        .dayOpenTime(parseTime(request.getDayOpenTime()))
        .nightOpenTime(parseTime(request.getNightOpenTime()))
        .closeTime(parseTime(request.getCloseTime()))
        .build();
  }

  public BoothMenu toBoothMenu(Booth booth, BoothMenuRequest request, String iconImageUrl) {
    return BoothMenu.builder()
        .booth(booth)
        .nameKo(request.getNameKo())
        .nameEn(request.getNameEn())
        .nameZh(request.getNameZh())
        .price(request.getPrice())
        .timeType(request.getTimeType())
        .isSoldOut(Boolean.TRUE.equals(request.getSoldOut()))
        .descriptionKo(request.getDescriptionKo())
        .category(request.getCategory())
        .iconImageUrl(iconImageUrl)
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
      Booth booth,
      BoothTranslation translation,
      List<BoothDetailImage> detailImages,
      List<BoothOperation> operations,
      List<BoothMenu> menus) {
    LocalDate today = LocalDate.now();
    LocalTime now = LocalTime.now();
    Optional<BoothOperation> todayOperation =
        operations.stream()
            .filter(operation -> operation.getOperationDate().equals(today))
            .findFirst();
    boolean open = todayOperation.map(operation -> operation.isOpenAt(now)).orElse(false);
    boolean orderAvailable =
        booth.isOrderEnabled()
            && todayOperation.map(operation -> operation.isOpenAt(now)).orElse(false);

    return BoothResponse.builder()
        .boothId(booth.getId())
        .thumbnailUrl(booth.getThumbnailUrl())
        .open(open)
        .orderAvailable(orderAvailable)
        .location(booth.getLocation())
        .locationDescription(getLocationDescription(booth))
        .boothNumbers(booth.getBoothNumbers())
        .departmentName(translation.getDepartmentName())
        .boothName(translation.getBoothName())
        .description(translation.getDescription())
        .detailImages(toBoothDetailImageResponses(detailImages))
        .menus(toBoothMenuSummaryGroupResponse(menus))
        .build();
  }

  public BoothAccountResponse toBoothAccountResponse(Booth booth) {
    return BoothAccountResponse.builder()
        .accountName(booth.getAccountName())
        .accountNumber(booth.getAccountNumber())
        .bankName(booth.getBankName())
        .build();
  }

  public BoothOperationResponse toBoothOperationResponse(BoothOperation operation) {
    return BoothOperationResponse.builder()
        .operationDate(operation.getOperationDate())
        .timeType(operation.getTimeType())
        .dayOpenTime(operation.getDayOpenTime())
        .nightOpenTime(operation.getNightOpenTime())
        .closeTime(operation.getCloseTime())
        .build();
  }

  public BoothMenuResponse toBoothMenuResponse(BoothMenu menu) {
    return BoothMenuResponse.builder()
        .menuId(menu.getId())
        .name(menu.getNameKo())
        .price(menu.getPrice())
        .timeType(menu.getTimeType())
        .soldOut(menu.getIsSoldOut())
        .description(menu.getDescriptionKo())
        .category(menu.getCategory())
        .iconImageUrl(menu.getIconImageUrl())
        .build();
  }

  public OrderAvailableBoothMenuGroupResponse toOrderAvailableBoothMenuGroupResponse(
      List<BoothMenu> menus, Language language) {
    return OrderAvailableBoothMenuGroupResponse.builder()
        .main(toOrderAvailableBoothMenuResponses(menus, language, MenuCategory.MAIN))
        .side(toOrderAvailableBoothMenuResponses(menus, language, MenuCategory.SIDE))
        .drink(toOrderAvailableBoothMenuResponses(menus, language, MenuCategory.DRINK))
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

  private BoothMenuSummaryGroupResponse toBoothMenuSummaryGroupResponse(List<BoothMenu> menus) {
    if (menus == null || menus.isEmpty()) {
      return BoothMenuSummaryGroupResponse.builder().day(null).night(null).build();
    }

    List<BoothMenuSummaryResponse> dayMenus =
        menus.stream()
            .filter(
                menu -> menu.getTimeType() == TimeType.DAY || menu.getTimeType() == TimeType.ALL)
            .map(this::toBoothMenuSummaryResponse)
            .toList();
    List<BoothMenuSummaryResponse> nightMenus =
        menus.stream()
            .filter(
                menu -> menu.getTimeType() == TimeType.NIGHT || menu.getTimeType() == TimeType.ALL)
            .map(this::toBoothMenuSummaryResponse)
            .toList();

    return BoothMenuSummaryGroupResponse.builder()
        .day(dayMenus.isEmpty() ? null : dayMenus)
        .night(nightMenus.isEmpty() ? null : nightMenus)
        .build();
  }

  private BoothMenuSummaryResponse toBoothMenuSummaryResponse(BoothMenu menu) {
    return BoothMenuSummaryResponse.builder().name(menu.getNameKo()).price(menu.getPrice()).build();
  }

  private List<OrderAvailableBoothMenuResponse> toOrderAvailableBoothMenuResponses(
      List<BoothMenu> menus, Language language, MenuCategory category) {
    return menus.stream()
        .filter(menu -> menu.getCategory() == category)
        .map(menu -> toOrderAvailableBoothMenuResponse(menu, language))
        .toList();
  }

  private OrderAvailableBoothMenuResponse toOrderAvailableBoothMenuResponse(
      BoothMenu menu, Language language) {
    return OrderAvailableBoothMenuResponse.builder()
        .menuId(menu.getId())
        .iconImageUrl(menu.getIconImageUrl())
        .name(language.getMenuName(menu))
        .description(menu.getDescriptionKo())
        .price(menu.getPrice())
        .soldOut(menu.getIsSoldOut())
        .build();
  }

  private String getLocationDescription(Booth booth) {
    if (booth.getLocation() == null) {
      return null;
    }
    return booth.getLocation().getDescription();
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

  public LocalDate parseDate(String date) {
    if (date == null || date.isBlank()) {
      return null;
    }

    try {
      return LocalDate.parse(date, DATE_FORMATTER);
    } catch (DateTimeParseException e) {
      throw new CustomException(BoothErrorCode.INVALID_OPERATION_DATE_FORMAT);
    }
  }
}
