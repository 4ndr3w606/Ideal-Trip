package com.web.spring.ideal_trip.security;

import com.web.spring.ideal_trip.model.Usuario;
import com.web.spring.ideal_trip.model.enums.Rol;
import com.web.spring.ideal_trip.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Spring llama a este método después de que el usuario autoriza en Google.
     * Recibe los atributos del perfil (email, nombre, etc.) y debe devolver
     * un OAuth2User que represente al usuario autenticado en NUESTRO sistema.
     *
     * Reglas:
     *   - Si el email ya existe en BD: lo reutilizamos.
     *   - Si NO existe: lo creamos con rol CLIENTE, password aleatorio.
     *   - Si está desactivado: bloqueamos el login.
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        String email = (String) attributes.get("email");
        String nombreRaw = (String) attributes.get("given_name");
        String apellidoRaw = (String) attributes.get("family_name");
        String nombre = (nombreRaw == null || nombreRaw.isBlank()) ? "Usuario" : nombreRaw;
        String apellido = (apellidoRaw == null || apellidoRaw.isBlank()) ? "Google" : apellidoRaw;

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_email"),
                    "Google no devolvió un email para esta cuenta");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseGet(() -> registrarNuevo(email, nombre, apellido));

        if (!usuario.isActivo()) {
            log.warn("Login OAuth bloqueado para usuario desactivado: {}", email);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_disabled"),
                    "La cuenta está desactivada");
        }

        // Autoridad según el rol del usuario en NUESTRO sistema, no en Google.
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));

        // nameAttributeKey="email" hace que principal.getName() devuelva el email,
        // consistente con el flujo tradicional. Así los controllers existentes
        // (Principal principal → usuarioService.buscarPorEmail(principal.getName()))
        // funcionan sin cambios para ambos métodos de login.
        return new DefaultOAuth2User(authorities, attributes, "email");
    }

    private Usuario registrarNuevo(String email, String nombre, String apellido) {
        log.info("Creando usuario nuevo desde OAuth Google: {}", email);

        // Password aleatorio: el usuario nunca lo usará (entra solo por Google),
        // pero el campo es @NotBlank en la entidad.
        String passwordAleatorio = passwordEncoder.encode(UUID.randomUUID().toString());

        Usuario nuevo = Usuario.builder()
                .nombre(nombre.isBlank() ? "Usuario" : nombre)
                .apellido(apellido.isBlank() ? "Google" : apellido)
                .email(email)
                .password(passwordAleatorio)
                .rol(Rol.CLIENTE)
                .activo(true)
                .build();

        return usuarioRepository.save(nuevo);
    }
}