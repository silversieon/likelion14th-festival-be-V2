/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "LostItemPageResponse DTO", description = "분실물 목록 페이지 응답")
public class LostItemPageResponse {

  @Schema(description = "분실물 리스트")
  private List<LostItemListResponse> content;

  @Schema(description = "전체 분실물 개수", example = "20")
  private Long totalElements;

  @Schema(description = "전체 페이지 개수", example = "5")
  private Integer totalPages;

  @Schema(description = "현재 페이지 번호", example = "1")
  private Integer pageNum;

  @Schema(description = "페이지 크기", example = "4")
  private Integer pageSize;

  @Schema(description = "마지막 페이지 여부", example = "false")
  private Boolean last;
}
