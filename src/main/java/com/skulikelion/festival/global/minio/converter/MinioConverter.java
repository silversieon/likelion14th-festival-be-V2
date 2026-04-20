/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.minio.converter;

import com.skulikelion.festival.global.minio.dto.MinioResponseDTO;

public class MinioConverter {

  public static MinioResponseDTO.ImgUrlDTO toImgUrlDTO(String imgUrl) {
    return MinioResponseDTO.ImgUrlDTO.builder().imageUrl(imgUrl).build();
  }
}
