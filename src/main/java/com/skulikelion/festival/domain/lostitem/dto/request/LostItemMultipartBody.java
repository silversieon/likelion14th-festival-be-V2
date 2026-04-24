/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.lostitem.dto.request;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LostItemMultipartBody {

  @Schema(implementation = LostItemRequest.class)
  private LostItemRequest dto;

  @Schema(type = "string", format = "binary")
  private MultipartFile file;
}
