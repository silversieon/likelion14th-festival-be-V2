/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.dto.response.menu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BoothMenuTranslationResponse(
    String nameKo,
    String nameEn,
    String nameZh,
    String descriptionKo,
    String descriptionEn,
    String descriptionZh) {}
