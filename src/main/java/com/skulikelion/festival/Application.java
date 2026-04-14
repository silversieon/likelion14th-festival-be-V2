package com.skulikelion.festival;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.skulikelion.festival.global.config.property")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
