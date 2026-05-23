package com.web.spring.ideal_trip.controller.api;

import com.web.spring.ideal_trip.dto.api.LoginRequestDto;
import com.web.spring.ideal_trip.dto.api.LoginResponseDto;
import com.web.spring.ideal_trip.exception.RecursoNoEncontradoException;
import com.web.spring.ideal_trip.model.Usuario;
import com.web.spring.ideal_trip.security.JwtService;
import com.web.spring.ideal_trip.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login y emisión de tokens JWT")
public class AuthApiController {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesión y obtener token JWT",
            description = """
                    Valida las credenciales contra la base de datos y, si son correctas,
                    devuelve un JWT firmado válido por 24 horas. Pegá ese token en el
                    botón **Authorize** (arriba) con el formato `Bearer <token>` para
                    probar endpoints protegidos.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso; token devuelto"),
            @ApiResponse(responseCode = "400", description = "Body inválido (email mal formado o password vacío)"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas o cuenta desactivada")
    })
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {

        Usuario usuario;
        try {
            usuario = usuarioService.buscarPorEmail(request.getEmail());
        } catch (RecursoNoEncontradoException ex) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        if (!usuario.isActivo()) {
            throw new BadCredentialsException("Cuenta desactivada");
        }

        String token = jwtService.generarToken(usuario.getEmail(), usuario.getRol().name());

        return ResponseEntity.ok(new LoginResponseDto(
                token,
                usuario.getEmail(),
                usuario.getRol().name(),
                usuario.getNombre() + " " + usuario.getApellido()
        ));
    }
}