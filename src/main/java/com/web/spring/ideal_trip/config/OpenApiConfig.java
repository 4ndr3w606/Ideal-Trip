package com.web.spring.ideal_trip.config;

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
    public OpenAPI idealTripOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ideal-Trip API")
                        .version("1.0.0")
                        .description("""
                                API REST de la agencia de viajes Ideal-Trip.

                                **Autenticación:** los endpoints públicos (catálogo) no requieren token.
                                Para los protegidos, hacé login en `POST /api/auth/login`, copiá el token
                                devuelto, y pegalo arriba en el botón **Authorize** con el formato:

                                    Bearer <token>
                                """)
                        .contact(new Contact()
                                .name("Felipe")
                                .email("u20241222745@usco.edu.co")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Token JWT emitido por POST /api/auth/login")));
    }
}