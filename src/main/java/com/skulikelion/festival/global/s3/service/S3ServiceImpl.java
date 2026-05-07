/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.s3.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
  private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
  private static final String DEFAULT_IMAGE_EXTENSION = "jpg";
  private static final int MAX_IMAGE_DIMENSION = 1280;
  private static final float JPEG_QUALITY = 0.7f;

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
      CompressedImage compressed = compressImage(file);
      String keyName = createKeyName(pathName, compressed.extension());
      uploadToS3(keyName, compressed);

      log.info(
          "[S3] 파일 업로드 성공 - keyName: {}, originalSize={} bytes, compressedSize={} bytes",
          keyName,
          file.getSize(),
          compressed.data().length);
      return createBucketImageUrl(keyName);
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

  private void uploadToS3(String keyName, CompressedImage compressed) {
    s3Client.putObject(
        PutObjectRequest.builder()
            .bucket(awsProperties.getS3().getBucket())
            .key(keyName)
            .contentType(compressed.contentType())
            .contentLength((long) compressed.data().length)
            .build(),
        RequestBody.fromBytes(compressed.data()));
  }

  private CompressedImage compressImage(MultipartFile file) throws IOException {
    // MultipartFile -> BufferedImage 로 변환
    // ImageIO가 읽을 수 있는 포맷이면 리사이징 및 JPG 압축 진행
    BufferedImage original = ImageIO.read(file.getInputStream());

    if (original == null) {
      // ImageIO가 읽지 못하는 포맷(HEIC 등)은 원본 그대로 업로드
      log.warn("[S3] 이미지 압축 불가 (미지원 포맷) - contentType: {}", file.getContentType());
      return new CompressedImage(
          file.getBytes(),
          extractFileExtension(file),
          Objects.requireNonNullElse(file.getContentType(), "image/jpeg"));
    }

    // 리사이즈 및 JPG 변환 준비
    BufferedImage prepared = prepareForJpeg(original);

    // 원본 이미지 참조 제거 -> GC 대상
    original = null;

    // 최종 JPG byte 배열 생성
    byte[] jpegBytes = writeAsJpeg(prepared);
    return new CompressedImage(jpegBytes, DEFAULT_IMAGE_EXTENSION, "image/jpeg");
  }

  private BufferedImage prepareForJpeg(BufferedImage source) {
    int srcWidth = source.getWidth();
    int srcHeight = source.getHeight();

    int destWidth = srcWidth;
    int destHeight = srcHeight;

    // 원본 이미지가 최대 크기를 초과하면 비율 유지하며 축소
    if (srcWidth > MAX_IMAGE_DIMENSION || srcHeight > MAX_IMAGE_DIMENSION) {
      double scale =
          Math.min(
              (double) MAX_IMAGE_DIMENSION / srcWidth, (double) MAX_IMAGE_DIMENSION / srcHeight);
      destWidth = (int) (srcWidth * scale);
      destHeight = (int) (srcHeight * scale);
    }

    // JPG 저장을 위해 RGB 타입으로 변환
    BufferedImage result = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = result.createGraphics();
    // 이미지 축소 시 픽셀 깨짐을 줄이기 위한 보간 옵션
    g.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(source, 0, 0, destWidth, destHeight, null);
    g.dispose();
    return result;
  }

  private byte[] writeAsJpeg(BufferedImage image) throws IOException {
    // JPG 포맷 Writer 조회
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
    ImageWriter writer = writers.next();
    ImageWriteParam param = writer.getDefaultWriteParam();
    // 압축 품질 직접 지정
    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    param.setCompressionQuality(JPEG_QUALITY);

    // 메모리 상에서 JPG byte 배열 생성
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
      writer.setOutput(ios);
      writer.write(null, new IIOImage(image, null, null), param);
    } finally {
      writer.dispose();
    }
    return baos.toByteArray();
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

  private String extractFileExtension(MultipartFile file) {
    String originalFilename = file.getOriginalFilename();

    if (originalFilename == null || !originalFilename.contains(".")) {
      return DEFAULT_IMAGE_EXTENSION;
    }

    return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
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

  private record CompressedImage(byte[] data, String extension, String contentType) {}
}
