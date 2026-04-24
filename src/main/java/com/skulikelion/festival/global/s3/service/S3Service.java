/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.s3.service;

import org.springframework.web.multipart.MultipartFile;

import com.skulikelion.festival.global.s3.enums.PathName;

public interface S3Service {

  /**
   * 이미지를 넣을 경로명과 타입 입력시 저장될 경로명을 문자열로 반환하는 메서드
   *
   * @param pathName s3 폴더(경로)명
   * @param fileType 저장할 이미지 타입
   * @return 저장될 경로 문자열
   */
  String createKeyName(PathName pathName, String fileType);

  /**
   * 이미지 파일을 S3 버킷에 업로드 하는 메서드
   *
   * @param pathName 이미지를 넣을 폴더 경로
   * @param file 이미지
   * @return 업로드한 이미지의 s3 url
   */
  String uploadFile(PathName pathName, MultipartFile file);

  /**
   * 이미지를 바이트 형식으로 S3 버킷에 업로드하는 메서드
   *
   * @param pathName 이미지를 넣을 폴더 경로
   * @param bytes 이미지의 바이트 형식
   * @return 업로드한 이미지의 s3 url
   */
  String uploadByte(PathName pathName, byte[] bytes);

  /**
   * keyName을 인자로 받아 해당 이미지 파일을 s3에서 삭제하는 메서드
   *
   * @param keyName keyName
   */
  void deleteFile(String keyName);

  /**
   * imageUrl을 인자로 받아 keyName 문자열을 반환하는 메서드
   *
   * @param imageUrl s3 이미지 url
   * @return keyName 문자열
   */
  String extractKeyNameFromUrl(String imageUrl);
}
