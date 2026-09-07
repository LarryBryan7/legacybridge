package com.legacybridge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI legacyBridgeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("LegacyBridge API")
                .description("Enterprise Legacy Integration Gateway - Fase 1: nucleo de dominio Operation")
                .version("v0.1.0"));
    }
}
