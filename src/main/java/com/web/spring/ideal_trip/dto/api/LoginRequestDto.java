package com.web.spring.ideal_trip.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Credenciales para iniciar sesión")
public class LoginRequestDto {

    @NotBlank
    @Email
    @Schema(description = "Email registrado", example = "admin@ideal-trip.com")
    private String email;

    @NotBlank
    @Schema(description = "Contraseña en texto plano (se compara contra el hash BCrypt)",
            example = "admin123")
    private String password;
}