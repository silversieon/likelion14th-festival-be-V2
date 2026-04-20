/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "TokenResponse: 토큰 리프레시 성공 응답 DTO [ 운영에서 삭제 필요 ]")
public class TokenResponse {

  @Schema(description = "JWT 액세스 토큰")
  private String accessToken;

  @Schema(description = "JWT 리프레시 토큰")
  private String refreshToken;
}
