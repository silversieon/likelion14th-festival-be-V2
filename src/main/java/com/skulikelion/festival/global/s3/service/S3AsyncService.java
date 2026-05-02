/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.s3.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.s3.enums.PathName;
import com.skulikelion.festival.global.s3.exception.S3ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class S3AsyncService {

  private final S3Service s3Service;
  private final Executor s3UploadExecutor;

  public S3AsyncService(
      S3Service s3Service, @Qualifier("s3UploadExecutor") Executor s3UploadExecutor) {
    this.s3Service = s3Service;
    this.s3UploadExecutor = s3UploadExecutor;
  }

  public List<String> uploadFiles(PathName pathName, List<MultipartFile> files) {
    List<MultipartFile> validFiles =
        files == null
            ? List.of()
            : files.stream().filter(file -> file != null && !file.isEmpty()).toList();

    List<CompletableFuture<String>> futures =
        validFiles.stream().map(file -> uploadFileAsync(pathName, file)).toList();

    try {
      return futures.stream().map(CompletableFuture::join).toList();

    } catch (CompletionException e) {
      Throwable cause = e.getCause();

      if (cause instanceof CustomException customException) {
        throw customException;
      }

      log.error("[S3AsyncService] S3 병렬 업로드 실패 - pathName: {}", pathName, e);
      throw new CustomException(S3ErrorCode.FILE_SERVER_ERROR);

    } catch (Exception e) {
      log.error("[S3AsyncService] S3 병렬 업로드 실패 - pathName: {}", pathName, e);
      throw new CustomException(S3ErrorCode.FILE_SERVER_ERROR);
    }
  }

  public String uploadFile(PathName pathName, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }

    return s3Service.uploadFile(pathName, file);
  }

  private CompletableFuture<String> uploadFileAsync(PathName pathName, MultipartFile file) {
    return CompletableFuture.supplyAsync(
        () -> s3Service.uploadFile(pathName, file), s3UploadExecutor);
  }
}
