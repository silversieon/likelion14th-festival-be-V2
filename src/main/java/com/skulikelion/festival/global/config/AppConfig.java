/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skulikelion.festival.global.config.property.AppProperties;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class AppConfig {

  private final AppProperties appProperties;

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  public String instanceId() {
    return appProperties.getInstanceId();
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
