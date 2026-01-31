package com.lingo.lingoproject.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiSwaggerConfig {

  @Bean
  public OpenAPI ringoOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Ringo API")
            .description("REST API 문서화")
            .version("v1.0.0"))
        .components(
            new Components()
            .addSecuritySchemes("Authentication",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT"))
        )
        .addSecurityItem(new SecurityRequirement().addList("Authentication"));
  }

}
