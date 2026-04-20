/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan(basePackages = "com.skulikelion.festival.global.config.property")
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
