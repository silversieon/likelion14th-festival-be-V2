/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.skulikelion.festival.global.config.property.SwaggerProperties;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {

  private final SwaggerProperties swaggerProperties;

  @Bean
  public OpenAPI customOpenAPI() {
    Server server =
        new Server()
            .url(swaggerProperties.getUrl())
            .description(swaggerProperties.getName() + " Server");

    return new OpenAPI()
        .addServersItem(server)
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .info(
            new Info()
                .title("SKU LikeLion Festival API 명세서")
                .version("1.0")
                .description(
                    """
                                        # 멋쟁이사자처럼 서경대학교 축제 프로젝트 API 문서

                                        ## 주의사항
                                        - 파일 업로드 크기 제한: 10MB (1개 파일 크기)

                                        """));
  }

  @Bean
  public GroupedOpenApi apiGroup() {
    return GroupedOpenApi.builder().group("api").pathsToMatch("/**").build();
  }
}
