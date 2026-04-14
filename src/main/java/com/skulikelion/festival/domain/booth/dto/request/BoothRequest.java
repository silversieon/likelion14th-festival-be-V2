package com.skulikelion.festival.domain.booth.dto.request;

import com.skulikelion.festival.domain.booth.entity.OpeningHours;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "BoothRequest DTO", description = "부스 정보 관리를 위한 데이터 전송")
public class BoothRequest {

  @NotBlank(message = "부스 이름 항목은 필수입니다.")
  @Schema(description = "부스 이름", example = "디자인학부")
  private String name;

  @NotBlank(message = "비밀번호 항목은 필수입니다.")
  @Pattern(
      regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@$!%*?&])[A-Za-z\\d$@$!%*?&]{8,}$",
      message = "비밀번호는 최소 8자 이상, 숫자 및 특수문자를 포함해야 합니다.")
  @Schema(description = "비밀번호", example = "password123!")
  private String password;

  @NotNull(message = "영업 시간 항목은 필수입니다.")
  @Schema(
      description = "영업 시간",
      example = "DAY",
      allowableValues = {"DAY", "NIGHT", "FULL"})
  private OpeningHours openingHours;

  @Schema(description = "부스 운영 학과의 인스타그램 아이디", example = "sku.design_")
  private String boothInstagram;

  @Schema(description = "예약 서비스 사용 여부", example = "true")
  private Boolean serviceAgreement;

  @Schema(description = "부스 운영 학과(한국어)", example = "디자인학부")
  private String boothFacultyKo;

  @Schema(description = "부스 이름(한국어)", example = "디자인 주점 오픈")
  private String boothTitleKo;

  @Schema(description = "부스 소개글(한국어)", example = "디자인학과의 컨셉은 ㅁㅁ다. 부스 소개글 블라블라~")
  private String boothDescriptionKo;

  @Schema(description = "부스 위치(한국어)", example = "은주2관 좌측")
  private String boothLocationKo;

  @Schema(description = "부스 운영 학과(영어)", example = "design")
  private String boothFacultyEn;

  @Schema(description = "부스 이름(영어)", example = "design restaurant open!")
  private String boothTitleEn;

  @Schema(description = "부스 소개글(영어)", example = "hello~ blah blah~")
  private String boothDescriptionEn;

  @Schema(description = "부스 위치(영어)", example = "EunjuHall2 - Left Side")
  private String boothLocationEn;

  @Schema(description = "부스 운영 학과(중국어)", example = "设计")
  private String boothFacultyCh;

  @Schema(description = "부스 이름(중국어)", example = "设计餐厅开业了！")
  private String boothTitleCh;

  @Schema(description = "부스 소개글(중국어)", example = "哈喽～啦啦啦～")
  private String boothDescriptionCh;

  @Schema(description = "부스 위치(중국어)", example = "恩珠2馆 左侧")
  private String boothLocationCh;

  @Schema(description = "부스 운영 학과(일본어)", example = "デザイン")
  private String boothFacultyJp;

  @Schema(description = "부스 이름(일본어)", example = "デザインレストランがオープンしました！")
  private String boothTitleJp;

  @Schema(description = "부스 소개글(일본어)", example = "こんにちは〜 なんちゃって〜")
  private String boothDescriptionJp;

  @Schema(description = "부스 위치(일본어)", example = "恩珠2館 左側")
  private String boothLocationJp;
}
