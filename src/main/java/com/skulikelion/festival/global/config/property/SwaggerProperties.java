package com.skulikelion.festival.global.config.property;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ConfigurationProperties("swagger.server")
public class SwaggerProperties {

    private String url;
    private String name;
}
