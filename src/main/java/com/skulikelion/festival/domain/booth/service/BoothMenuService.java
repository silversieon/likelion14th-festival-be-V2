/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.booth.dto.request.BoothMenuRequest;
import com.skulikelion.festival.domain.booth.dto.response.BoothMenuResponse;
import com.skulikelion.festival.domain.booth.entity.Booth;
import com.skulikelion.festival.domain.booth.entity.BoothMenu;
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.domain.booth.exception.MenuErrorCode;
import com.skulikelion.festival.domain.booth.mapper.BoothMapper;
import com.skulikelion.festival.domain.booth.repository.BoothMenuRepository;
import com.skulikelion.festival.domain.booth.repository.BoothRepository;
import com.skulikelion.festival.global.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoothMenuService {

  private final BoothRepository boothRepository;
  private final BoothMenuRepository boothMenuRepository;
  private final BoothMapper boothMapper;

  @Transactional
  public BoothMenuResponse createBoothMenu(BoothMenuRequest request) {

    // name으로 booth 찾기
    Booth booth =
        boothRepository
            .findByName(request.getName())
            .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));

    // 해당 부스에 같은 메뉴가 있는지 검사
    boolean isExist =
        boothMenuRepository.existsByBoothIdAndMenuKo(booth.getId(), request.getMenuKo());
    if (isExist) {
      throw new CustomException(MenuErrorCode.MENU_ALREADY_EXISTS);
    }

    // menu 등록
    BoothMenu boothMenu =
        BoothMenu.builder()
            .booth(booth)
            .menuKo(request.getMenuKo())
            .menuEn(request.getMenuEn())
            .menuCh(request.getMenuCh())
            .menuJp(request.getMenuJp())
            .menuPrice(request.getMenuPrice())
            .menuTimeType(request.getMenuTimeType())
            .build();

    BoothMenu savedMenu = boothMenuRepository.save(boothMenu);

    log.info(
        "[메뉴 생성 POST: /api/dev/booths/menus] 부스 이름: {}, 메뉴 이름: {}",
        booth.getName(),
        savedMenu.getMenuKo());

    return boothMapper.toBoothMenuResponse(savedMenu);
  }

  @Transactional
  public BoothMenuResponse updateBoothMenu(BoothMenuRequest request) {

    // name으로 booth 찾기
    Booth booth =
        boothRepository
            .findByName(request.getName())
            .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));

    // boothMenu와 일치하는 메뉴 찾기
    BoothMenu boothMenu =
        boothMenuRepository
            .findByBoothIdAndMenuKo(booth.getId(), request.getBoothMenu())
            .orElseThrow(() -> new CustomException(MenuErrorCode.MENU_NOT_FOUND));

    boothMenu.update(
        boothMenu.getBooth(),
        request.getMenuKo(),
        request.getMenuEn(),
        request.getMenuCh(),
        request.getMenuJp(),
        request.getMenuPrice(),
        request.getMenuTimeType());

    log.info(
        "[메뉴 수정 PUT: /api/dev/booths/menus] 부스 이름: {}, 수정된 메뉴 이름: {}",
        boothMenu.getBooth().getName(),
        boothMenu.getMenuKo());

    return boothMapper.toBoothMenuResponse(boothMenu);
  }

  @Transactional
  public void deleteBoothMenu(String boothFaculty, String menuMame) {

    // name으로 booth 찾기
    Booth booth =
        boothRepository
            .findByName(boothFaculty)
            .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));
    // 해당 부스의 메뉴 중 menuKo(한국어 메뉴)와 일치하는 메뉴 찾기
    BoothMenu boothMenu =
        boothMenuRepository
            .findByBoothIdAndMenuKo(booth.getId(), menuMame)
            .orElseThrow(() -> new CustomException(MenuErrorCode.MENU_NOT_FOUND));

    boothMenuRepository.delete(boothMenu);

    log.info(
        "[메뉴 삭제 DELETE: /api/dev/booths/menus?faculty={}&name={}] 부스 이름: {}, 삭제된 메뉴 이름: {}",
        boothMenu.getBooth().getName(),
        boothMenu.getMenuKo(),
        boothMenu.getBooth().getName(),
        boothMenu.getMenuKo());
  }
}
