/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "order.event")
public record OrderEventProperties(Pattern pattern) {

  public enum Pattern {
    DIRECT,
    OUTBOX
  }
}
