/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ConfigurationProperties("cors")
public class CorsProperties {

  private String[] allowedOrigins;
}
