/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.request.booth;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.skulikelion.festival.domain.booth.enums.BoothLocation;
import com.skulikelion.festival.global.enums.Department;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "BoothRequest: 부스 생성/수정 요청 DTO")
public class BoothRequest {

  @NotNull(message = "학과를 입력해주세요.") @Schema(description = "학과", example = "SOFTWARE")
  private Department department;

  @Schema(description = "주문 서비스 사용 여부", example = "true")
  private Boolean orderEnabled;

  @Schema(description = "부스 위치", example = "DAEIL")
  private BoothLocation location;

  @Schema(description = "부스 번호 목록", example = "[1, 2]")
  private List<Integer> boothNumbers;

  @Schema(description = "예금주", example = "홍길동")
  private String accountName;

  @Schema(description = "계좌번호", example = "123-456-789")
  private String accountNumber;

  @Schema(description = "은행명", example = "국민은행")
  private String bankName;

  @Valid
  @NotEmpty(message = "부스 운영 정보를 입력해주세요.")
  @Schema(description = "부스 운영 정보, 3개 이상")
  private List<BoothOperationRequest> operations;

  @Valid
  @NotEmpty(message = "부스 번역 정보를 입력해주세요.")
  @Schema(description = "부스 번역 정보, KO/EN/ZH 모두 필수")
  private List<BoothTranslationRequest> translations;
}
