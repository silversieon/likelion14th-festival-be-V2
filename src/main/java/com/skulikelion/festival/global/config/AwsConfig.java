/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.skulikelion.festival.global.config.property.AwsProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Getter
@Configuration
@RequiredArgsConstructor
public class AwsConfig {

  private final AwsProperties awsProperties;

  @Bean
  public AwsCredentialsProvider awsCredentialsProvider() {
    return StaticCredentialsProvider.create(
        AwsBasicCredentials.create(
            awsProperties.getCredentials().getAccessKey(),
            awsProperties.getCredentials().getSecretKey()));
  }

  @Bean
  public S3Client s3Client(AwsCredentialsProvider awsCredentialsProvider) {
    return S3Client.builder()
        .region(Region.of(awsProperties.getRegionStatic()))
        .credentialsProvider(awsCredentialsProvider)
        .build();
  }
}
