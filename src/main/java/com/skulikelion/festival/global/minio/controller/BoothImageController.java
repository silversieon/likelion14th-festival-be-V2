/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.minio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.skulikelion.festival.global.minio.converter.MinioConverter;
import com.skulikelion.festival.global.minio.dto.MinioResponseDTO;
import com.skulikelion.festival.global.minio.entity.PathName;
import com.skulikelion.festival.global.minio.service.MinioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/image/booths")
@Tag(name = "Booth Image Controller", description = "부스 이미지 업로드 API")
public class BoothImageController {

  private final MinioService minioService;

  @Operation(summary = "부스 이미지 업로드", description = "booth 경로로 이미지를 업로드하고 URL을 반환")
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<MinioResponseDTO.ImgUrlDTO> uploadBoothImage(
      @Parameter(description = "업로드할 이미지 파일", required = true) @RequestParam("file")
          MultipartFile file) {
    String url = minioService.uploadFile(file, PathName.BOOTH);
    return ResponseEntity.ok(MinioConverter.toImgUrlDTO(url));
  }

  @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "부스 이미지 여러 개 업로드", description = "여러 이미지를 booth 경로에 업로드하고 URL 리스트를 반환")
  public ResponseEntity<List<MinioResponseDTO.ImgUrlDTO>> uploadMultipleBoothImages(
      @RequestParam("files") List<MultipartFile> files) {
    List<MinioResponseDTO.ImgUrlDTO> response =
        files.stream()
            .map(file -> MinioConverter.toImgUrlDTO(minioService.uploadFile(file, PathName.BOOTH)))
            .toList();
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "이미지 삭제", description = "버킷에서 파일을 삭제합니다.")
  @DeleteMapping("/image-delete")
  public ResponseEntity<String> deleteImage(@RequestParam String keyName) {
    try {
      minioService.deleteFile(keyName); // 파일 삭제 처리
      return ResponseEntity.status(HttpStatus.OK).body("파일 삭제 성공"); // 200 OK와 함께 성공 메시지 반환
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("파일 삭제 실패"); // 예외 발생 시 실패 응답 반환
    }
  }
}
