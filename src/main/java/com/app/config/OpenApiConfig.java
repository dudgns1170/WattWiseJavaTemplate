package com.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI(Swagger) 설정 클래스
 * 
 * API 문서화를 위한 Swagger UI 설정을 정의합니다.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI wattWiseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WattWise Backend API")
                        .description("WattWise 백엔드 REST API 문서")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("WattWise Team")
                                .email("contact@example.com")
                        )
                );
    }
}
