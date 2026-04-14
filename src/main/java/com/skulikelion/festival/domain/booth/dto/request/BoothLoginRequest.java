package com.skulikelion.festival.domain.booth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "BoothLoginRequest DTO", description = "부스 관리자 로그인을 위한 데이터 전송")
public class BoothLoginRequest {

  @NotBlank(message = "부스 이름 항목은 필수입니다.")
  @Schema(description = "부스 이름", example = "디자인학부")
  private String name;

  @NotBlank(message = "비밀번호 항목은 필수입니다.")
  @Schema(description = "비밀번호", example = "password123!")
  private String password;
}
