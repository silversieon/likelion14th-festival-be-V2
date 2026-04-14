package com.skulikelion.festival.domain.booth.dto.response;

import com.skulikelion.festival.domain.booth.entity.OpeningHours;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(
    title = "BoothDetailInfoResponse DTO",
    description = "언어 설정에 맞춰서(ko, en, ch, jp) 부스의 자세한 정보 및 메뉴에 대한 응답 반환")
public class BoothDetailInfoResponse {

  @Schema(description = "부스 ID", example = "1")
  private Long id;

  @Schema(description = "부스 운영 학과", example = "디자인학부")
  private String boothFaculty;

  @Schema(description = "부스 이름", example = "디자인 주점 오픈")
  private String boothTitle;

  @Schema(description = "부스 소개글", example = "디자인학과의 컨셉은 ㅁㅁ다. 부스 소개글 블라블라~")
  private String boothDescription;

  @Schema(description = "부스 운영 학과의 인스타그램 아이디", example = "sku.design_")
  private String boothInstagram;

  @Schema(description = "이미지 URL 리스트")
  private List<String> imageUrls;

  @Schema(description = "부스 운영 시간대", example = "DAY")
  private OpeningHours openingHours;

  @Schema(description = "예약 서비스 사용 여부", example = "true")
  private Boolean serviceAgreement;

  @Schema(
      description = "부스 메뉴",
      example =
          """
              [{\
              menu : "Spicy Fish Cake",\
              menuKR : "매운어묵탕",\
              menuPrice : 5000\
              }]\
              
              언어설정 한국어 : menu와 menuPrice만 반환\
              
              언어설정 외국어 : menu, menuKR, menuPrice 반환""")
  private List<InBoothMenuResponse> boothMenus;
}
