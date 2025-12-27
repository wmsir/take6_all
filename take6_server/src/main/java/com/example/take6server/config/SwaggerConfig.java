package com.example.take6server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI take6OpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Take6 服务器 API")
                        .description("Take6 服务器应用程序的 API 文档")
                        .version("v1.0"));
    }
}
