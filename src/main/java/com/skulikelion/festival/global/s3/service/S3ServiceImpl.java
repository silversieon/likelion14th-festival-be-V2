/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.s3.service;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import com.skulikelion.festival.global.config.property.AwsProperties;
import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.s3.enums.PathName;
import com.skulikelion.festival.global.s3.exception.S3ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

  private static final String PREFIX_S3_URL = "https://";
  private static final String INFIX_S3_URL = ".s3.";
  private static final String SUFFIX_S3_URL = ".amazonaws.com/";
  private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
  private static final int WEBP_QUALITY = 80;
  private static final String WEBP_EXTENSION = "webp";
  private static final String WEBP_CONTENT_TYPE = "image/webp";

  private final S3Client s3Client;
  private final AwsProperties awsProperties;

  @Override
  public String createKeyName(PathName pathName, String fileType) {
    return getPrefix(pathName) + "/" + UUID.randomUUID() + "." + fileType;
  }

  @Override
  public String uploadFile(PathName pathName, MultipartFile file) {
    validateFile(file);

    try {
      Path webpTempFile = convertToWebpTempFile(file.getInputStream());

      try {
        String keyName = createKeyName(pathName, WEBP_EXTENSION);
        uploadToS3(keyName, webpTempFile);

        log.info("[S3] 파일 업로드 성공 - keyName: {}, size={} bytes", keyName, Files.size(webpTempFile));
        return createBucketImageUrl(keyName);
      } finally {
        Files.deleteIfExists(webpTempFile);
      }
    } catch (CustomException e) {
      throw e;
    } catch (Exception e) {
      log.error("[S3] 파일 업로드 실패", e);
      throw new CustomException(S3ErrorCode.FILE_SERVER_ERROR);
    }
  }

  @Override
  public void deleteFile(String keyName) {
    fileExists(keyName);

    try {
      s3Client.deleteObject(
          DeleteObjectRequest.builder()
              .bucket(awsProperties.getS3().getBucket())
              .key(keyName)
              .build());

      log.info("[S3] 이미지 삭제 성공 - keyName: {}", keyName);
    } catch (Exception e) {
      log.error("[S3] 이미지 삭제 실패 - keyName: {}", keyName, e);
      throw new CustomException(S3ErrorCode.FILE_SERVER_ERROR);
    }
  }

  @Override
  public String extractKeyNameFromUrl(String imageUrl) {
    if (imageUrl == null || !imageUrl.startsWith(getBucketUrl())) {
      log.error("[S3] 유효하지 않은 이미지 URL - imageUrl: {}", imageUrl);
      throw new CustomException(S3ErrorCode.FILE_URL_INVALID);
    }

    String keyName = imageUrl.substring(getBucketUrl().length());
    log.info("[S3] keyName 추출 성공 - keyName: {}", keyName);
    return keyName;
  }

  private void uploadToS3(String keyName, byte[] bytes) {
    s3Client.putObject(
        PutObjectRequest.builder()
            .bucket(awsProperties.getS3().getBucket())
            .key(keyName)
            .contentType(WEBP_CONTENT_TYPE)
            .contentLength((long) bytes.length)
            .build(),
        RequestBody.fromBytes(bytes));
  }

  private void uploadToS3(String keyName, Path filePath) throws java.io.IOException {
    s3Client.putObject(
        PutObjectRequest.builder()
            .bucket(awsProperties.getS3().getBucket())
            .key(keyName)
            .contentType(WEBP_CONTENT_TYPE)
            .contentLength(Files.size(filePath))
            .build(),
        RequestBody.fromFile(filePath));
  }

  private void fileExists(String keyName) {
    try {
      s3Client.headObject(
          HeadObjectRequest.builder()
              .bucket(awsProperties.getS3().getBucket())
              .key(keyName)
              .build());
    } catch (NoSuchKeyException e) {
      log.error("[S3] 존재하지 않는 파일 - keyName: {}", keyName);
      throw new CustomException(S3ErrorCode.FILE_NOT_FOUND);
    } catch (Exception e) {
      log.error("[S3] 파일 존재 여부 확인 실패 - keyName: {}", keyName, e);
      throw new CustomException(S3ErrorCode.FILE_SERVER_ERROR);
    }
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new CustomException(S3ErrorCode.FILE_NOT_FOUND);
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new CustomException(S3ErrorCode.FILE_SIZE_INVALID);
    }

    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new CustomException(S3ErrorCode.FILE_TYPE_INVALID);
    }
  }

  private String createBucketImageUrl(String keyName) {
    return getBucketUrl() + keyName;
  }

  private String getBucketUrl() {
    return PREFIX_S3_URL
        + awsProperties.getS3().getBucket()
        + INFIX_S3_URL
        + awsProperties.getRegionStatic()
        + SUFFIX_S3_URL;
  }

  private String getPrefix(PathName pathName) {
    return switch (pathName) {
      case BOOTH_THUMBNAIL -> awsProperties.getS3().getPath().getBoothThumbnail();
      case BOOTH_DETAIL -> awsProperties.getS3().getPath().getBoothDetail();
      case LOST_ITEM -> awsProperties.getS3().getPath().getLostItem();
      case COMMON_ICON -> awsProperties.getS3().getPath().getCommonIcon();
    };
  }

  private Path convertToWebpTempFile(InputStream inputStream) {
    try {
      ImmutableImage image = ImmutableImage.loader().fromStream(inputStream);
      image = normalizeImageType(image);

      WebpWriter writer = WebpWriter.DEFAULT.withQ(WEBP_QUALITY);
      Path tempFile = Files.createTempFile("s3-upload-", "." + WEBP_EXTENSION);
      image.forWriter(writer).write(tempFile);
      return tempFile;
    } catch (CustomException e) {
      throw e;
    } catch (Exception e) {
      log.error("[S3] WebP 임시 파일 변환 실패 - 유효하지 않은 이미지 또는 변환 실패", e);
      throw new CustomException(S3ErrorCode.FILE_TYPE_INVALID);
    }
  }

  private ImmutableImage normalizeImageType(ImmutableImage image) {
    BufferedImage src = image.awt();
    int targetType =
        src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_3BYTE_BGR;

    if (src.getType() == targetType) {
      return image;
    }

    BufferedImage converted = new BufferedImage(src.getWidth(), src.getHeight(), targetType);
    Graphics2D graphics = converted.createGraphics();
    graphics.setComposite(AlphaComposite.Src);
    graphics.drawImage(src, 0, 0, null);
    graphics.dispose();
    return ImmutableImage.fromAwt(converted);
  }
}
