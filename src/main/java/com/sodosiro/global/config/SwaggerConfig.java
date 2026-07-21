package com.sodosiro.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI) 설정.
 * 문서 UI: http://localhost:${SERVER_PORT:8080}/swagger-ui/index.html
 * 스펙(JSON): http://localhost:${SERVER_PORT:8080}/v3/api-docs
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("sodosiro API")
                        .description("여행지 추천 백엔드 API 문서")
                        .version("v0.0.1"));
        // TODO: JWT 인증 붙으면 SecurityScheme(bearer) 추가
    }
}
