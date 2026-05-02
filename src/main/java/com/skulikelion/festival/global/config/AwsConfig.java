/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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

  @Bean(name = "s3UploadExecutor")
  public Executor s3UploadExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(6);
    executor.setQueueCapacity(30);
    executor.setThreadNamePrefix("s3-upload-");

    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

    executor.initialize();
    return executor;
  }
}
