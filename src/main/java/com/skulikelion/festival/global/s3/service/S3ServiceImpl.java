/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.s3.service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
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
  private static final int MAX_IMAGE_DIMENSION = 1280;

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
      ProcessedImage image = processImage(file);
      String keyName = createKeyName(pathName, image.extension());

      uploadToS3(keyName, image);

      log.info(
          "[S3] 파일 업로드 성공 - keyName: {}, originalSize={} bytes, resizedSize={} bytes",
          keyName,
          file.getSize(),
          image.data().length);

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

    return imageUrl.substring(getBucketUrl().length());
  }

  private void uploadToS3(String keyName, ProcessedImage image) {
    s3Client.putObject(
        PutObjectRequest.builder()
            .bucket(awsProperties.getS3().getBucket())
            .key(keyName)
            .contentType(image.contentType())
            .build(),
        RequestBody.fromBytes(image.data()));
  }

  private ProcessedImage processImage(MultipartFile file) throws IOException {
    String extension = extractFileExtension(file);
    String contentType = Objects.requireNonNullElse(file.getContentType(), "image/jpeg");

    BufferedImage image = ImageIO.read(file.getInputStream());

    if (image == null) {
      log.warn("[S3] 이미지 처리 불가 - contentType: {}", file.getContentType());
      return new ProcessedImage(file.getBytes(), extension, contentType);
    }

    image = applyExifOrientation(image, readOrientation(file));
    image = resizeImage(image);

    return new ProcessedImage(writeImage(image, extension), extension, contentType);
  }

  private BufferedImage applyExifOrientation(BufferedImage image, int orientation) {
    return switch (orientation) {
      case 3 -> rotateImage(image, 180);
      case 6 -> rotateImage(image, 90);
      case 8 -> rotateImage(image, -90);
      default -> image;
    };
  }

  private int readOrientation(MultipartFile file) {
    try {
      Metadata metadata = ImageMetadataReader.readMetadata(file.getInputStream());
      ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

      if (directory == null || !directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
        return 1;
      }

      return directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);

    } catch (Exception e) {
      log.debug("[S3] EXIF Orientation 읽기 실패 - fileName: {}", file.getOriginalFilename(), e);
      return 1;
    }
  }

  private BufferedImage resizeImage(BufferedImage source) {
    int width = source.getWidth();
    int height = source.getHeight();

    if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
      return source;
    }

    double scale =
        Math.min((double) MAX_IMAGE_DIMENSION / width, (double) MAX_IMAGE_DIMENSION / height);

    int resizedWidth = (int) (width * scale);
    int resizedHeight = (int) (height * scale);

    BufferedImage result =
        new BufferedImage(resizedWidth, resizedHeight, BufferedImage.TYPE_INT_RGB);

    Graphics2D g = result.createGraphics();
    g.drawImage(source, 0, 0, resizedWidth, resizedHeight, null);
    g.dispose();

    return result;
  }

  private BufferedImage rotateImage(BufferedImage source, int angle) {
    int width = source.getWidth();
    int height = source.getHeight();

    boolean swapSize = angle == 90 || angle == -90;

    BufferedImage result =
        new BufferedImage(
            swapSize ? height : width, swapSize ? width : height, BufferedImage.TYPE_INT_RGB);

    Graphics2D g = result.createGraphics();

    if (angle == 90) {
      g.translate(height, 0);
    } else if (angle == -90) {
      g.translate(0, width);
    } else {
      g.translate(width, height);
    }

    g.rotate(Math.toRadians(angle));
    g.drawImage(source, 0, 0, null);
    g.dispose();

    return result;
  }

  private byte[] writeImage(BufferedImage image, String extension) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    if (!ImageIO.write(image, extension.equals("jpeg") ? "jpg" : extension, baos)) {
      throw new CustomException(S3ErrorCode.FILE_TYPE_INVALID);
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
      return "jpg";
    }

    String extension =
        originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();

    return extension.equals("jpeg") ? "jpg" : extension;
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

  private record ProcessedImage(byte[] data, String extension, String contentType) {}
}
