package com.web.spring.ideal_trip.security;

import com.web.spring.ideal_trip.model.Usuario;
import com.web.spring.ideal_trip.model.enums.Rol;
import com.web.spring.ideal_trip.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOidcUserService extends OidcUserService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Para providers OIDC (Google, Microsoft) — se invoca cuando el scope incluye 'openid'.
     */
    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        String nombreRaw = oidcUser.getGivenName();
        String apellidoRaw = oidcUser.getFamilyName();

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_email"),
                    "Google no devolvió un email para esta cuenta");
        }

        String nombre = (nombreRaw == null || nombreRaw.isBlank()) ? "Usuario" : nombreRaw;
        String apellido = (apellidoRaw == null || apellidoRaw.isBlank()) ? "Google" : apellidoRaw;

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseGet(() -> registrarNuevo(email, nombre, apellido));

        if (!usuario.isActivo()) {
            log.warn("Login OIDC bloqueado para usuario desactivado: {}", email);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_disabled"),
                    "La cuenta está desactivada");
        }

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));

        // nameAttributeKey="email" → principal.getName() devuelve el email
        return new DefaultOidcUser(
                authorities,
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "email"
        );
    }

    private Usuario registrarNuevo(String email, String nombre, String apellido) {
        log.info("Creando usuario nuevo desde OIDC Google: {}", email);
        String passwordAleatorio = passwordEncoder.encode(UUID.randomUUID().toString());
        Usuario nuevo = Usuario.builder()
                .nombre(nombre)
                .apellido(apellido)
                .email(email)
                .password(passwordAleatorio)
                .rol(Rol.CLIENTE)
                .activo(true)
                .build();
        return usuarioRepository.save(nuevo);
    }
}