/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.skulikelion.festival.domain.order.service.idempotency.IdempotencyService;
import com.skulikelion.festival.global.config.property.IdempotencyProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class IdempotencyConfig {

  private final IdempotencyProperties properties;

  @Bean
  @Primary
  public IdempotencyService IdempotencyService(Map<String, IdempotencyService> implementations) {
    String beanName = properties.getStore() + "IdempotencyService";
    IdempotencyService selected = implementations.get(beanName);

    if (selected == null) {
      throw new IllegalStateException(
          "알 수 없는 idempotency.store 값: " + properties.getStore() + " (가능한 값: redis, db, fallback)");
    }
    log.info("선택된 구체 클래스: {}", selected);
    return selected;
  }
}
