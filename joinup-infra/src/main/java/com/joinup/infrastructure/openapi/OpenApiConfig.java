package com.joinup.infrastructure.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Knife4j 文档配置。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI joinupOpenApi() {
        // 统一声明 Bearer 鉴权方案，避免每个 controller 单独重复定义。
        String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("JoinUp API")
                        .description("JoinUp backend API document")
                        .version("v0.2.0")
                        .contact(new Contact().name("JoinUp Team")))
                .components(new Components().addSecuritySchemes(
                        schemeName,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                .addSecurityItem(new SecurityRequirement().addList(schemeName));
    }
}
