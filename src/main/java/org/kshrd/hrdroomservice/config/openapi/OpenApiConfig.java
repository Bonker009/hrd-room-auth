package org.kshrd.hrdroomservice.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("HRD Room Service API")
                                .version("v4")
                                .description(
                                        "REST API for HRD room service operations, including authentication, "
                                                + "academic years, classrooms, courses, and enrollments. "
                                                + "Obtain a JWT from POST /api/v4/auth/login; "
                                                + "send it as Bearer authentication on protected routes.")
                                .contact(
                                        new Contact()
                                                .name("HRD Room Service Team")
                                                .email("penhseyha4980@gmail.com")))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "bearerAuth",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")));
    }
}
