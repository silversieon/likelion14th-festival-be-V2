/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
public abstract class IntegrationTestSupport {

  @ServiceConnection static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

  @ServiceConnection
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  static {
    MYSQL.start();
    REDIS.start();
  }
}
