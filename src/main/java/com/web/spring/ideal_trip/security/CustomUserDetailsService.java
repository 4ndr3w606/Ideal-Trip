package com.web.spring.ideal_trip.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.web.spring.ideal_trip.model.Usuario;
import com.web.spring.ideal_trip.repository.UsuarioRepository;


@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
               .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con emai: " + email));

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPassword())
                .roles(usuario.getRol().name())   // "CLIENTE" o "ADMIN"
                .disabled(!usuario.isActivo())     // si activo=false, no puede entrar
                .build();
    }

}
