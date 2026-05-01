/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.skulikelion.festival.domain.booth.dto.request.BoothMenuRequest;
import com.skulikelion.festival.domain.booth.dto.request.UpdateBoothMenuPriceRequest;
import com.skulikelion.festival.domain.booth.dto.request.UpdateBoothMenuSoldOutRequest;
import com.skulikelion.festival.domain.booth.dto.response.BoothMenuResponse;
import com.skulikelion.festival.domain.booth.dto.response.OrderAvailableBoothMenuGroupResponse;
import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.entity.BoothMenu;
import com.skulikelion.festival.domain.booth.entity.BoothOperation;
import com.skulikelion.festival.domain.booth.enums.TimeType;
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.domain.booth.mapper.BoothMapper;
import com.skulikelion.festival.domain.booth.repository.BoothMenuRepository;
import com.skulikelion.festival.domain.booth.repository.BoothOperationRepository;
import com.skulikelion.festival.domain.booth.repository.BoothRepository;
import com.skulikelion.festival.domain.manager.entity.Manager;
import com.skulikelion.festival.domain.manager.entity.enums.Role;
import com.skulikelion.festival.domain.manager.exception.ManagerErrorCode;
import com.skulikelion.festival.domain.manager.repository.ManagerRepository;
import com.skulikelion.festival.domain.order.exception.OrderErrorCode;
import com.skulikelion.festival.global.enums.Department;
import com.skulikelion.festival.global.enums.Language;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.s3.enums.PathName;
import com.skulikelion.festival.global.s3.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoothMenuServiceImpl implements BoothMenuService {

  private final BoothRepository boothRepository;
  private final BoothMenuRepository boothMenuRepository;
  private final BoothOperationRepository boothOperationRepository;
  private final ManagerRepository managerRepository;
  private final BoothMapper boothMapper;
  private final S3Service s3Service;

  @Override
  @Transactional
  public List<BoothMenuResponse> createMenus(
      Long boothId, List<BoothMenuRequest> requests, List<MultipartFile> iconImages) {
    log.info("[BoothMenuService] 메뉴 생성 요청 - 부스 식별자: {}, 요청 개수: {}", boothId, requests.size());
    List<MultipartFile> validIconImages =
        iconImages == null
            ? List.of()
            : iconImages.stream()
                .filter(iconImage -> iconImage != null && !iconImage.isEmpty())
                .toList();
    if (!validIconImages.isEmpty() && validIconImages.size() != requests.size()) {
      log.warn(
          "[BoothMenuService] 메뉴 생성 실패 - 메뉴 개수와 아이콘 이미지 개수 불일치, 메뉴 개수: {}, 이미지 개수: {}",
          requests.size(),
          validIconImages.size());
      throw new CustomException(BoothErrorCode.BOOTH_MENU_ICON_IMAGE_COUNT_MISMATCH);
    }
    Booth booth = getBooth(boothId);

    // 아이콘 업로드 및 메뉴 저장
    List<BoothMenu> boothMenus =
        IntStream.range(0, requests.size())
            .mapToObj(
                index -> {
                  BoothMenuRequest request = requests.get(index);
                  String iconImageUrl = uploadImage(getIconImage(validIconImages, index));
                  return boothMapper.toBoothMenu(booth, request, iconImageUrl);
                })
            .toList();
    List<BoothMenu> savedBoothMenus = boothMenuRepository.saveAll(boothMenus);
    log.info(
        "[BoothMenuService] 메뉴 생성 발생 - 부스 식별자: {}, 생성 개수: {}", boothId, savedBoothMenus.size());
    return savedBoothMenus.stream().map(boothMapper::toBoothMenuResponse).toList();
  }

  @Override
  @Transactional
  public BoothMenuResponse updateMenu(
      Long menuId, BoothMenuRequest request, MultipartFile iconImage) {
    log.info("[BoothMenuService] 메뉴 수정 요청 - 메뉴 식별자: {}, 메뉴명: {}", menuId, request.getNameKo());
    BoothMenu boothMenu = getBoothMenu(menuId);

    // 아이콘 전체 교체
    String oldIconImageUrl = boothMenu.getIconImageUrl();
    String iconImageUrl = uploadImage(iconImage);
    deleteImageQuietly(oldIconImageUrl);

    // 메뉴 기본 정보 교체
    boothMenu.update(
        request.getNameKo(),
        request.getNameEn(),
        request.getNameZh(),
        request.getPrice(),
        request.getTimeType(),
        request.getSoldOut(),
        request.getDescriptionKo(),
        request.getCategory(),
        iconImageUrl);
    log.info("[BoothMenuService] 메뉴 수정 발생 - 메뉴 식별자: {}", menuId);
    return boothMapper.toBoothMenuResponse(boothMenu);
  }

  @Override
  @Transactional
  public BoothMenuResponse updateMenuPrice(
      String departmentName, Long menuId, UpdateBoothMenuPriceRequest request) {
    log.info(
        "[BoothMenuService] 메뉴 가격 수정 요청 - 관리자 학과: {}, 메뉴 식별자: {}, 가격: {}",
        departmentName,
        menuId,
        request.getPrice());
    BoothMenu boothMenu = getBoothMenu(menuId);

    // 부스 관리자 권한 검증
    validateBoothManager(departmentName, boothMenu.getBooth());

    // 메뉴 가격 변경
    boothMenu.updatePrice(request.getPrice());
    log.info("[BoothMenuService] 메뉴 가격 수정 발생 - 메뉴 식별자: {}, 가격: {}", menuId, request.getPrice());
    return boothMapper.toBoothMenuResponse(boothMenu);
  }

  @Override
  @Transactional
  public BoothMenuResponse updateMenuSoldOut(
      String departmentName, Long menuId, UpdateBoothMenuSoldOutRequest request) {
    log.info(
        "[BoothMenuService] 메뉴 품절 여부 변경 요청 - 관리자 학과: {}, 메뉴 식별자: {}, 품절 여부: {}",
        departmentName,
        menuId,
        request.getSoldOut());
    BoothMenu boothMenu = getBoothMenu(menuId);

    // 부스 관리자 권한 검증
    validateBoothManager(departmentName, boothMenu.getBooth());

    // 메뉴 품절 여부 변경
    boothMenu.updateSoldOut(request.getSoldOut());
    log.info(
        "[BoothMenuService] 메뉴 품절 여부 변경 발생 - 메뉴 식별자: {}, 품절 여부: {}", menuId, request.getSoldOut());
    return boothMapper.toBoothMenuResponse(boothMenu);
  }

  @Override
  @Transactional(readOnly = true)
  public OrderAvailableBoothMenuGroupResponse getOrderAvailableMenus(
      Long boothId, Language language) {
    log.info("[BoothMenuService] 주문 가능 메뉴 조회 요청 - 부스 식별자: {}, 언어: {}", boothId, language);
    Booth booth = getBooth(boothId);

    // 주문 서비스 사용 여부 검증
    if (!booth.isOrderEnabled()) {
      log.warn("[BoothMenuService] 주문 미사용 부스 메뉴 조회 요청 - 부스 식별자: {}", boothId);
      throw new CustomException(OrderErrorCode.ORDER_NOT_USED_BOOTH);
    }

    // 현재 주문 가능 시간 검증
    BoothOperation operation =
        boothOperationRepository
            .findByBoothIdAndOperationDate(boothId, LocalDate.now())
            .orElse(null);
    if (operation == null) {
      log.info("[BoothMenuService] 주문 가능 메뉴 없음 - 오늘 운영 정보 없음, 부스 식별자: {}", boothId);
      return boothMapper.toOrderAvailableBoothMenuGroupResponse(List.of(), language);
    }
    LocalTime now = LocalTime.now();
    if (!operation.isOpenAt(now)) {
      log.warn("[BoothMenuService] 주문 가능 시간 외 메뉴 조회 요청 - 부스 식별자: {}, 현재 시간: {}", boothId, now);
      throw new CustomException(OrderErrorCode.NOT_TIME_TO_ORDER);
    }

    // 현재 시간대 메뉴 필터링
    TimeType currentTimeType = operation.getCurrentOrderTimeType(now);
    List<TimeType> allowedTypes = TimeType.forQuery(currentTimeType);
    List<BoothMenu> menus =
        boothMenuRepository.findByBoothIdOrderByIdAsc(boothId).stream()
            .filter(menu -> allowedTypes.contains(menu.getTimeType()))
            .toList();

    log.info(
        "[BoothMenuService] 주문 가능 메뉴 조회 발생 - 부스 식별자: {}, 현재 시간 타입: {}, 메뉴 개수: {}",
        boothId,
        currentTimeType,
        menus.size());
    return boothMapper.toOrderAvailableBoothMenuGroupResponse(menus, language);
  }

  @Override
  @Transactional
  public void deleteMenu(Long menuId) {
    log.info("[BoothMenuService] 메뉴 삭제 요청 - 메뉴 식별자: {}", menuId);
    BoothMenu boothMenu = getBoothMenu(menuId);

    // S3 아이콘 정리 및 메뉴 삭제
    deleteImageQuietly(boothMenu.getIconImageUrl());
    boothMenuRepository.delete(boothMenu);
    log.info("[BoothMenuService] 메뉴 삭제 발생 - 메뉴 식별자: {}", menuId);
  }

  private Booth getBooth(Long boothId) {
    return boothRepository
        .findById(boothId)
        .orElseThrow(
            () -> {
              log.warn("[BoothMenuService] 부스 조회 실패 - 존재하지 않는 부스 식별자: {}", boothId);
              return new CustomException(BoothErrorCode.BOOTH_NOT_FOUND);
            });
  }

  private BoothMenu getBoothMenu(Long menuId) {
    return boothMenuRepository
        .findById(menuId)
        .orElseThrow(
            () -> {
              log.warn("[BoothMenuService] 메뉴 조회 실패 - 메뉴 식별자: {}", menuId);
              return new CustomException(BoothErrorCode.BOOTH_MENU_NOT_FOUND);
            });
  }

  private void validateBoothManager(String departmentName, Booth booth) {
    Department department = Department.valueOf(departmentName);
    Manager currentManager =
        managerRepository
            .findByDepartment(department)
            .orElseThrow(() -> new CustomException(ManagerErrorCode.MANAGER_NOT_FOUND));

    if (!booth.getDepartment().equals(currentManager.getDepartment())
        && currentManager.getRole() != Role.ADMIN) {
      log.warn(
          "[BoothMenuService] 메뉴 접근 권한 검증 실패 - 관리자 학과: {}, 부스 학과: {}",
          currentManager.getDepartment(),
          booth.getDepartment());
      throw new CustomException(BoothErrorCode.BOOTH_ACCESS_DENIED);
    }
  }

  private String uploadImage(MultipartFile image) {
    if (image == null || image.isEmpty()) {
      log.info("[BoothMenuService] 메뉴 아이콘 업로드 생략");
      return null;
    }
    log.info("[BoothMenuService] 메뉴 아이콘 업로드 요청 - 파일명: {}", image.getOriginalFilename());
    return s3Service.uploadFile(PathName.COMMON_ICON, image);
  }

  private MultipartFile getIconImage(List<MultipartFile> iconImages, int index) {
    if (iconImages == null || iconImages.size() <= index) {
      return null;
    }
    return iconImages.get(index);
  }

  private void deleteImageQuietly(String imageUrl) {
    if (imageUrl == null || imageUrl.isBlank()) {
      return;
    }
    try {
      // S3 이미지 삭제
      String keyName = s3Service.extractKeyNameFromUrl(imageUrl);
      s3Service.deleteFile(keyName);
      log.info("[BoothMenuService] S3 이미지 삭제 완료 - imageUrl: {}", imageUrl);
    } catch (RuntimeException e) {
      log.warn("[BoothMenuService] S3 이미지 삭제 실패 - imageUrl: {}", imageUrl, e);
    }
  }
}
