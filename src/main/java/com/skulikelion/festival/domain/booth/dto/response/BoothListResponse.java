package com.skulikelion.festival.domain.booth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "BoothListResponse DTO", description = "부스 리스트 조회에 대한 응답 반환")
public class BoothListResponse {

  @Schema(description = "부스 ID", example = "1")
  private Long id;

  @Schema(description = "부스 운영 학과", example = "디자인학부")
  private String boothFaculty;

  @Schema(description = "부스 썸네일 사진 url")
  private String boothThumbnailUrl;

  @Schema(description = "부스 현재 대기팀 수", example = "14")
  private Integer boothWaitings;

  @Schema(description = "부스 위치", example = "은주2관 좌측")
  private String boothLocation;

  @Schema(description = "부스 영업 여부", example = "true")
  private Boolean working;

  @Schema(description = "예약 서비스 사용 여부", example = "true")
  private Boolean serviceAgreement;
}
