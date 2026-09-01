/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sse")
public record OrderSseProperties(SseStrategy strategy, Redis redis) {
  public enum SseStrategy {
    LOCAL,
    DISTRIBUTED
  }

  public enum SubscriptionMode {
    PATTERN,
    DYNAMIC
  }

  public record Redis(SubscriptionMode subscriptionMode) {}

  public OrderSseProperties {
    if (strategy == SseStrategy.LOCAL && redis != null && redis.subscriptionMode() != null) {
      throw new IllegalStateException(
          "sse.strategy가 'local'일 때는 sse.redis.subscription-mode를 설정할 수 없습니다. "
              + "설정을 제거하거나 sse.strategy를 'distributed'로 변경하세요.");
    }
    if (strategy != SseStrategy.DISTRIBUTED && redis != null && redis.subscriptionMode() != null) {
      throw new IllegalStateException(
          "sse.strategy가 'distributed'가 아닐 때는 sse.redis.subscription-mode를 설정할 수 없습니다. "
              + "설정을 제거하거나 sse.strategy를 'distributed'로 변경하세요.");
    }
  }
}
