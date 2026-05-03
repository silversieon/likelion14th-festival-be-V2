/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.response.booth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "BoothAccountResponse: 부스 입금 계좌 응답 DTO")
public class BoothAccountResponse {

  @Schema(description = "예금주")
  private String accountName;

  @Schema(description = "계좌번호")
  private String accountNumber;

  @Schema(description = "은행명")
  private String bankName;
}
