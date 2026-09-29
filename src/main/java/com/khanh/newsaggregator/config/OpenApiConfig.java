package com.khanh.newsaggregator.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuthentication";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("News Aggregator & Tracker API")
                        .description("Hệ thống backend tổng hợp, xử lý và theo dõi tin tức tự động xây dựng bằng Spring Boot 3, Java 21, PostgreSQL (Full-Text Search), Redis Cache và Spring Security JWT.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Tuan Khanh")
                                .email("khanhblue06@gmail.com")
                                .url("https://github.com/tuankhanh2306/News-Aggregator-Tracker"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập mã JWT token sau khi đăng nhập để xác thực các API bảo mật (ví dụ: /api/subscriptions/**)")));
    }
}
