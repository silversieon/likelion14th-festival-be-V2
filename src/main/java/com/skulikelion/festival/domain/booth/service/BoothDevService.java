/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.skulikelion.festival.domain.booth.dto.request.BoothRequest;
import com.skulikelion.festival.domain.booth.dto.response.BoothResponse;
import com.skulikelion.festival.domain.booth.entity.*;
import com.skulikelion.festival.domain.booth.exception.BoothErrorCode;
import com.skulikelion.festival.domain.booth.mapper.BoothMapper;
import com.skulikelion.festival.domain.booth.repository.*;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.minio.entity.PathName;
import com.skulikelion.festival.global.minio.service.MinioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoothDevService {

  private final BoothRepository boothRepository;
  private final BoothMapper boothMapper;
  private final BoothDetailChRepository chRepository;
  private final BoothDetailEnRepository enRepository;
  private final BoothDetailKoRepository koRepository;
  private final BoothDetailJpRepository jpRepository;
  private final MinioService minioService;

  /**
   * 부스 등록을 처리합니다.
   *
   * @param boothRequest 부스 등록 요청 정보 (부스 이름과 비밀번호 포함)
   * @return 등록된 부스 정보 응답 DTO
   * @throws CustomException 이미 존재하는 부스인 경우
   */
  @Transactional
  public BoothResponse createBooth(BoothRequest boothRequest, List<MultipartFile> images) {
    // 중복 부스명 확인
    if (boothRepository.findByName(boothRequest.getName()).isPresent()) {
      throw new CustomException(BoothErrorCode.BOOTH_ALREADY_EXISTS);
    }

    String thumbnailImgUrl = minioService.uploadFile(images.getFirst(), PathName.BOOTH);

    List<BoothImage> boothImages =
        images.stream()
            .map(
                file ->
                    BoothImage.builder()
                        .imageUrl(minioService.uploadFile(file, PathName.BOOTH))
                        .build())
            .toList();

    // Booth 저장
    Booth booth =
        Booth.builder()
            .name(boothRequest.getName())
            .password(boothRequest.getPassword())
            .waitingTeam(0)
            .openingHours(boothRequest.getOpeningHours())
            .boothImages(boothImages)
            .boothThumbnailUrl(thumbnailImgUrl)
            .boothInstagram(boothRequest.getBoothInstagram())
            .serviceAgreement(boothRequest.getServiceAgreement())
            .build();

    boothImages.forEach(image -> image.setBooth(booth));

    Booth savedBooth = boothRepository.save(booth);

    // BoothDetail 저장 (번역본)
    BoothDetailKo boothDetailKo =
        BoothDetailKo.builder()
            .booth(booth)
            .boothFacultyKo(boothRequest.getBoothFacultyKo())
            .boothTitleKo(boothRequest.getBoothTitleKo())
            .boothDescriptionKo(boothRequest.getBoothDescriptionKo())
            .boothLocationKo(boothRequest.getBoothLocationKo())
            .build();
    BoothDetailKo savedBoothDetailKo = koRepository.save(boothDetailKo);

    BoothDetailEn boothDetailEn =
        BoothDetailEn.builder()
            .booth(booth)
            .boothFacultyEn(boothRequest.getBoothFacultyEn())
            .boothTitleEn(boothRequest.getBoothTitleEn())
            .boothDescriptionEn(boothRequest.getBoothDescriptionEn())
            .boothLocationEn(boothRequest.getBoothLocationEn())
            .build();
    BoothDetailEn savedBoothDetailEn = enRepository.save(boothDetailEn);

    BoothDetailCh boothDetailCh =
        BoothDetailCh.builder()
            .booth(booth)
            .boothFacultyCh(boothRequest.getBoothFacultyCh())
            .boothTitleCh(boothRequest.getBoothTitleCh())
            .boothDescriptionCh(boothRequest.getBoothDescriptionCh())
            .boothLocationCh(boothRequest.getBoothLocationCh())
            .build();
    BoothDetailCh savedBoothDetailCh = chRepository.save(boothDetailCh);

    BoothDetailJp boothDetailJp =
        BoothDetailJp.builder()
            .booth(booth)
            .boothFacultyJp(boothRequest.getBoothFacultyJp())
            .boothTitleJp(boothRequest.getBoothTitleJp())
            .boothDescriptionJp(boothRequest.getBoothDescriptionJp())
            .boothLocationJp(boothRequest.getBoothLocationJp())
            .build();
    BoothDetailJp savedBoothDetailJp = jpRepository.save(boothDetailJp);

    log.info("[부스 생성 POST: /api/dev/booths] 부스 이름: {}", savedBooth.getName());

    return boothMapper.toBoothResponse(savedBooth);
  }

  /**
   * 모든 부스 정보를 조회합니다.
   *
   * @return 등록된 모든 부스의 응답 DTO 목록
   */
  @Transactional(readOnly = true)
  public List<BoothResponse> getAllBooths() {
    log.info("[부스 전체 조회 GET: /api/dev/booths] 전체 부스 목록 조회 요청");
    List<Booth> booths = boothRepository.findAll();
    return booths.stream().map(boothMapper::toBoothResponse).toList();
  }

  /**
   * 부스 정보를 수정합니다.
   *
   * @param boothRequest 부스 수정 요청 정보 (부스 이름과 비밀번호 포함)
   * @return 수정된 부스 정보 응답 DTO
   * @throws CustomException 부스를 찾을 수 없는 경우
   */
  @Transactional
  public BoothResponse updateBooth(BoothRequest boothRequest) {
    Booth booth =
        boothRepository
            .findByName(boothRequest.getName())
            .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));

    BoothDetailKo boothDetailKo =
        koRepository
            .findByBoothId(booth.getId())
            .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));

    BoothDetailEn boothDetailEn =
        enRepository
            .findByBoothId(booth.getId())
            .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));

    BoothDetailCh boothDetailCh =
        chRepository
            .findByBoothId(booth.getId())
            .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));

    BoothDetailJp boothDetailJp =
        jpRepository
            .findByBoothId(booth.getId())
            .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));

    booth.update(
        boothRequest.getName(),
        boothRequest.getPassword(),
        boothRequest.getOpeningHours(),
        boothRequest.getBoothInstagram());
    boothDetailKo.update(
        booth,
        boothRequest.getBoothFacultyKo(),
        boothRequest.getBoothTitleKo(),
        boothRequest.getBoothDescriptionKo(),
        boothRequest.getBoothLocationKo());
    boothDetailEn.update(
        booth,
        boothRequest.getBoothFacultyEn(),
        boothRequest.getBoothTitleEn(),
        boothRequest.getBoothDescriptionEn(),
        boothRequest.getBoothLocationEn());
    boothDetailCh.update(
        booth,
        boothRequest.getBoothFacultyCh(),
        boothRequest.getBoothTitleCh(),
        boothRequest.getBoothDescriptionCh(),
        boothRequest.getBoothLocationCh());
    boothDetailJp.update(
        booth,
        boothRequest.getBoothFacultyJp(),
        boothRequest.getBoothTitleJp(),
        boothRequest.getBoothDescriptionJp(),
        boothRequest.getBoothLocationJp());

    log.info("[부스 수정 PUT: /api/dev/booths] 수정된 부스 이름: {}", booth.getName());

    return boothMapper.toBoothResponse(booth);
  }

  @Transactional
  public void deleteBooth(Long id) {
    Booth booth =
        boothRepository
            .findById(id)
            .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));

    BoothDetailKo boothDetailKo =
        koRepository
            .findByBoothId(booth.getId())
            .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));

    BoothDetailEn boothDetailEn =
        enRepository
            .findByBoothId(booth.getId())
            .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));

    BoothDetailCh boothDetailCh =
        chRepository
            .findByBoothId(booth.getId())
            .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));

    BoothDetailJp boothDetailJp =
        jpRepository
            .findByBoothId(booth.getId())
            .orElseThrow(() -> new CustomException(BoothErrorCode.BOOTH_NOT_FOUND));

    boothRepository.delete(booth);
    koRepository.delete(boothDetailKo);
    enRepository.delete(boothDetailEn);
    chRepository.delete(boothDetailCh);
    jpRepository.delete(boothDetailJp);

    log.info("[부스 삭제 DELETE: /api/dev/booths] 삭제된 부스 이름: {}", booth.getName());
  }
}
