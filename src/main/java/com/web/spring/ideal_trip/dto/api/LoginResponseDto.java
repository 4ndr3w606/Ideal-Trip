package com.web.spring.ideal_trip.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Respuesta del login con el token JWT")
public class LoginResponseDto {

    @Schema(description = "Token JWT firmado (válido 24h)",
            example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Email del usuario autenticado", example = "admin@ideal-trip.com")
    private String email;

    @Schema(description = "Rol del usuario", example = "ADMIN",
            allowableValues = {"CLIENTE", "ADMIN"})
    private String rol;

    @Schema(description = "Nombre y apellido del usuario", example = "Felipe Carrillo")
    private String nombreCompleto;
}