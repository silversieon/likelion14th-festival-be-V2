package com.skulikelion.festival.domain.booth.controller;

import com.skulikelion.festival.domain.booth.dto.response.BoothDetailInfoResponse;
import com.skulikelion.festival.domain.booth.dto.response.BoothListResponse;
import com.skulikelion.festival.domain.booth.service.BoothInfoService;
import com.skulikelion.festival.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boothInfo")
@Tag(name = "BoothInfo", description = "BoothInfo 관리 API")
public class BoothInfoController {

  private final BoothInfoService boothInfoService;

  // 언어 설정에 따라 부스 리스트를 조회하며 커서 기반 페이징
  @Operation(summary = "[사용자] 부스 리스트 조회", description = "언어 설정에 따라 부스 리스트를 조회합니다. (200 OK)")
  @GetMapping
  public ResponseEntity<BaseResponse<List<BoothListResponse>>> getBoothList(
      @Parameter(description = "브라우저 언어 설정 (ko, en, ch, jp)", example = "ko") @RequestParam
      String lang,
      @Parameter(description = "마지막으로 조회한 부스 식별자", example = "5") @RequestParam(required = false)
      Long cursor,
      @Parameter(description = "예약 서비스 사용 여부", example = "true") @RequestParam(required = false)
      Boolean serviceAgreement) {

    List<BoothListResponse> boothListResponses = boothInfoService.getBoothList(lang, cursor,
        serviceAgreement);

    return ResponseEntity.ok(BaseResponse.success("부스 리스트 조회가 성공적으로 완료되었습니다.", boothListResponses));
  }

  // 언어 설정에 따라 다르게 보내도록
  // 특정 부스 조회 (메뉴도 함께 조회됨)
  @Operation(
      summary = "[사용자] 특정 부스 조회",
      description = "언어 설정에 따라 특정 부스를 조회합니다. 시간대에 따라 부스의 메뉴가 달라집니다. (200 OK)")
  @GetMapping("/{id}")
  public ResponseEntity<BaseResponse<BoothDetailInfoResponse>> getBoothDetail(
      @Parameter(description = "조회할 부스의 식별자", example = "1") @PathVariable Long id,
      @Parameter(description = "브라우저 언어 설정 (ko, en, ch, jp)", example = "ko") @RequestParam
      String lang) {
    BoothDetailInfoResponse boothDetailInfoResponse = boothInfoService.getBoothById(id, lang);

    return ResponseEntity.ok(
        BaseResponse.success("부스 단건 조회가 성공적으로 완료되었습니다.", boothDetailInfoResponse));
  }

  @Operation(summary = "[사용자] 부스 학과명 검색", description = "언어 설정과 검색어를 통해 부스를 조회합니다. (200 OK)")
  @GetMapping("/search")
  public ResponseEntity<BaseResponse<List<BoothListResponse>>> searchBooths(
      @Parameter(description = "검색할 학과 이름", example = "전자") @RequestParam String facultyName,
      @Parameter(description = "브라우저 언어 설정 (ko, en, ch, jp)", example = "ko")
      @RequestParam(defaultValue = "ko")
      String lang,
      @Parameter(description = "예약 서비스 사용 여부", example = "true") @RequestParam(required = false)
      Boolean serviceAgreement) {
    List<BoothListResponse> boothSearchResponse = boothInfoService.searchBooths(lang, facultyName,
        serviceAgreement);
    return ResponseEntity.ok(BaseResponse.success("부스 검색이 성공적으로 완료되었습니다.", boothSearchResponse));
  }
}
