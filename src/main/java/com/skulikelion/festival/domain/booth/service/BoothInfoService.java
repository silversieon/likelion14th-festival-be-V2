package com.skulikelion.festival.domain.booth.service;

import com.skulikelion.festival.domain.booth.dto.response.BoothDetailInfoResponse;
import com.skulikelion.festival.domain.booth.dto.response.BoothListResponse;
import com.skulikelion.festival.domain.booth.dto.response.InBoothMenuResponse;
import com.skulikelion.festival.domain.booth.entity.*;
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.domain.booth.mapper.BoothMapper;
import com.skulikelion.festival.domain.booth.repository.*;
import com.skulikelion.festival.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoothInfoService {

  private final BoothRepository boothRepository;
  private final BoothDetailChRepository chRepository;
  private final BoothDetailEnRepository enRepository;
  private final BoothDetailKoRepository koRepository;
  private final BoothDetailJpRepository jpRepository;
  private final BoothMenuRepository boothMenuRepository;

  // 언어 설정에 따라 다르게 보내도록
  // 부스 리스트 조회
  @Transactional(readOnly = true)
  public List<BoothListResponse> getBoothList(String lang, Long cursor, Boolean serviceAgreement) {
    Pageable pageable = PageRequest.of(0, 8, Sort.by("id").ascending());
    Page<Booth> boothPage;

    if (Boolean.TRUE.equals(serviceAgreement)) {
      boothPage =
          (cursor == null)
              ? boothRepository.findByServiceAgreementTrue(pageable)
              : boothRepository.findByServiceAgreementTrueAndIdGreaterThan(cursor, pageable);
    } else {
      boothPage =
          (cursor == null)
              ? boothRepository.findAll(pageable)
              : boothRepository.findByIdGreaterThan(cursor, pageable);
    }

    List<Booth> booths = boothPage.getContent();

    List<BoothListResponse> boothListResponses = setBoothLang(booths, lang);

    log.info(
        "[부스 리스트 조회 GET: /api/boothInfo] 언어: {}, 커서: {}, 총 부스 수: {}",
        lang,
        cursor,
        boothListResponses.size());

    return boothListResponses;
  }

  // 언어 설정에 따라 다르게 보내도록
  // 특정 부스 조회 (메뉴도 함께 조회됨)
  @Transactional(readOnly = true)
  public BoothDetailInfoResponse getBoothById(Long id, String lang) {

    Booth booth =
        boothRepository
            .findById(id)
            .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));

    String faculty, title, description;

    // 언어 설정에 따라 학과명, 부스명, 부스설명글 설정
    switch (lang.toLowerCase()) {
      case "en" -> {
        BoothDetailEn en =
            enRepository
                .findByBoothId(id)
                .orElseThrow(() -> new IllegalArgumentException("영어 부스 정보 없음"));
        faculty = en.getBoothFacultyEn();
        title = en.getBoothTitleEn();
        description = en.getBoothDescriptionEn();
      }
      case "ch" -> {
        BoothDetailCh ch =
            chRepository
                .findByBoothId(id)
                .orElseThrow(() -> new IllegalArgumentException("중국어 부스 정보 없음"));
        faculty = ch.getBoothFacultyCh();
        title = ch.getBoothTitleCh();
        description = ch.getBoothDescriptionCh();
      }
      case "jp" -> {
        BoothDetailJp jp =
            jpRepository
                .findByBoothId(id)
                .orElseThrow(() -> new IllegalArgumentException("일본어 부스 정보 없음"));
        faculty = jp.getBoothFacultyJp();
        title = jp.getBoothTitleJp();
        description = jp.getBoothDescriptionJp();
      }
      case "ko" -> {
        BoothDetailKo ko =
            koRepository
                .findByBoothId(id)
                .orElseThrow(() -> new IllegalArgumentException("한국어 부스 정보 없음"));
        faculty = ko.getBoothFacultyKo();
        title = ko.getBoothTitleKo();
        description = ko.getBoothDescriptionKo();
      }
      default -> throw new IllegalArgumentException("유효하지 않은 언어 : " + lang.toLowerCase());
    }

    // 메뉴 조회

//    OpeningHours menuTimeType = getTimeType();

    List<InBoothMenuResponse> menuResponses;

    // 시간에 따라 (DAY / NIGHT) + FULL 에 해당하는 메뉴 반환
    List<BoothMenu> menus = boothMenuRepository.findByBoothId(id);
    menuResponses =
        menus.stream()
//            .filter(
//                menu ->
//                    menu.getMenuTimeType() == menuTimeType
//                        || menu.getMenuTimeType() == OpeningHours.FULL)
            .map(menu -> new InBoothMenuResponse(menu, lang))
            .toList();

    log.info("[부스 단일 조회 GET: /api/boothInfo/id] 부스 ID: {}, 언어: {}, 학과: {}", id, lang, faculty);

    return BoothMapper.toBoothDetailInfoResponse(booth, faculty, title, description, menuResponses);
  }

  // 검색으로 부스 조회
  @Transactional(readOnly = true)
  public List<BoothListResponse> searchBooths(String lang, String facultyName,
      Boolean serviceAgreement) {
    List<Long> boothIds;

    boothIds =
        switch (lang.toLowerCase()) {
          case "en" -> enRepository.findByBoothFacultyEnContaining(facultyName).stream()
              .map(booth -> booth.getBooth().getId())
              .toList();
          case "ch" -> chRepository.findByBoothFacultyChContaining(facultyName).stream()
              .map(booth -> booth.getBooth().getId())
              .toList();
          case "jp" -> jpRepository.findByBoothFacultyJpContaining(facultyName).stream()
              .map(booth -> booth.getBooth().getId())
              .toList();
          case "ko" -> koRepository.findByBoothFacultyKoContaining(facultyName).stream()
              .map(booth -> booth.getBooth().getId())
              .toList();
          default -> throw new IllegalArgumentException("유효하지 않은 언어 설정: " + lang);
        };

    // Booth 전체 조회 후 필터링
    List<Booth> booths = boothRepository.findAllByServiceAgreementAndIdIn(serviceAgreement,
        boothIds);
    List<BoothListResponse> boothSearchResponse = setBoothLang(booths, lang);

    log.info(
        "[부스 리스트 조회 GET: /api/boothInfo/search?faculty={}&lang={}] 언어: {}, 검색어: {}, 총 부스 수: {}",
        facultyName,
        lang,
        lang,
        facultyName,
        boothSearchResponse.size());

    return boothSearchResponse;
  }

  private List<BoothListResponse> setBoothLang(List<Booth> booths, String lang) {
    List<BoothListResponse> result = new ArrayList<>();

    LocalTime now = LocalTime.now();
    LocalTime dayTime = LocalTime.of(12, 0);
    LocalTime nightTime = LocalTime.of(18, 0);
    LocalTime endTime = LocalTime.of(22, 30);

    for (Booth booth : booths) {
      boolean working = false;

      if (now.isAfter(dayTime) && now.isBefore(nightTime)) {
        working =
            booth.getOpeningHours() == OpeningHours.DAY
                || booth.getOpeningHours() == OpeningHours.FULL;
      } else if (now.isAfter(nightTime) && now.isBefore(endTime)) {
        working =
            booth.getOpeningHours() == OpeningHours.NIGHT
                || booth.getOpeningHours() == OpeningHours.FULL;
      }

      Long id = booth.getId();

      String boothFaculty = "", boothLocation = "";

      // 언어 설정에 따라 학과명, 부스 위치 설정
      switch (lang.toLowerCase()) {
        case "ko" -> {
          BoothDetailKo ko = koRepository.findByBoothId(id).orElse(null);
          if (ko != null) {
            boothFaculty = ko.getBoothFacultyKo();
            boothLocation = ko.getBoothLocationKo();
          }
        }
        case "en" -> {
          BoothDetailEn en = enRepository.findByBoothId(id).orElse(null);
          if (en != null) {
            boothFaculty = en.getBoothFacultyEn();
            boothLocation = en.getBoothLocationEn();
          }
        }
        case "ch" -> {
          BoothDetailCh ch = chRepository.findByBoothId(id).orElse(null);
          if (ch != null) {
            boothFaculty = ch.getBoothFacultyCh();
            boothLocation = ch.getBoothLocationCh();
          }
        }
        case "jp" -> {
          BoothDetailJp jp = jpRepository.findByBoothId(id).orElse(null);
          if (jp != null) {
            boothFaculty = jp.getBoothFacultyJp();
            boothLocation = jp.getBoothLocationJp();
          }
        }
        default -> throw new IllegalStateException("유효하지 않은 언어: " + lang.toLowerCase());
      }

      BoothListResponse response =
          BoothMapper.toBoothListResponse(booth, boothFaculty, boothLocation, working);
      result.add(response);
    }

    return result;
  }

//  public static OpeningHours getTimeType() {
//    LocalTime now = LocalTime.now();
//
//    if (now.isAfter(LocalTime.of(5, 59)) && now.isBefore(LocalTime.of(18, 0))) {
//      return OpeningHours.DAY; // 06:00 ~ 17:59 => 낮 메뉴 반환
//    } else {
//      return OpeningHours.NIGHT; // 18:00 ~ 05:59 => 밤 메뉴 반환
//    }
//  }
}
