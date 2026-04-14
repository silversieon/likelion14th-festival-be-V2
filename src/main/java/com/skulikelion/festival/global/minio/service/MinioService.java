package com.skulikelion.festival.global.minio.service;

import com.skulikelion.festival.global.minio.config.MinioConfig;
import com.skulikelion.festival.global.minio.converter.MinioConverter;
import com.skulikelion.festival.global.minio.dto.MinioResponseDTO;
import com.skulikelion.festival.global.minio.entity.PathName;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioService {

  private final MinioClient minioClient;
  private final MinioConfig minioConfig;

  public MinioResponseDTO.ImgUrlDTO imgUpload(MultipartFile file) {
    String url = uploadFile(file, PathName.LOSTITEM);
    return MinioConverter.toImgUrlDTO(url);
  }

  public String uploadFile(MultipartFile file, PathName pathName) {
    // 1) 파일 유효성 검증
    if (file.getSize() > 10 * 1024 * 1024) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 크기는 10MB를 초과할 수 없습니다.");
    }
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "이미지 파일만 업로드 가능합니다.");
    }

    // 2) 버킷에 저장할 키 생성,버킷에 저장할 짧은 key 생성 (8자리 UUID)
    String extension =
        file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
    String key =
        pathName.name().toLowerCase()
            + "/"
            + UUID.randomUUID().toString().substring(0, 4)
            + extension;

    try {
      // 3) 업로드
      minioClient.putObject(
          PutObjectArgs.builder().bucket(minioConfig.getBucket()).object(key).stream(
                  file.getInputStream(), file.getSize(), -1)
              .contentType(contentType)
              .build());

      return "https://minio.2025skufestival.site/" + minioConfig.getBucket() + "/" + key;

    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드 중 서버 오류가 발생했습니다.", e);
    }
  }

  public String getFileUrl(String key) {
    try {
      // 존재 확인
      minioClient.statObject(
          StatObjectArgs.builder().bucket(minioConfig.getBucket()).object(key).build());
    } catch (MinioException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "요청하신 파일을 찾을 수 없습니다.");
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "파일 조회 중 서버 오류가 발생했습니다.", e);
    }

    try {
      // presigned URL 생성 (72시간 유효)
      return minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.GET)
              .bucket(minioConfig.getBucket())
              .object(key)
              .expiry(72 * 60 * 60)
              .build());
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "다운로드 URL 생성 중 오류가 발생했습니다.", e);
    }
  }

  public void deleteFile(String key) {
    try {
      // 존재 확인
      minioClient.statObject(
          StatObjectArgs.builder().bucket(minioConfig.getBucket()).object(key).build());
      // 삭제
      minioClient.removeObject(
          RemoveObjectArgs.builder().bucket(minioConfig.getBucket()).object(key).build());
    } catch (MinioException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "이미 삭제되었거나 존재하지 않는 파일입니다.");
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제 중 서버 오류가 발생했습니다.", e);
    }
  }
}
