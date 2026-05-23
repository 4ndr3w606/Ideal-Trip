package com.web.spring.ideal_trip.service;

import com.web.spring.ideal_trip.exception.RecursoNoEncontradoException;
import com.web.spring.ideal_trip.model.Usuario;
import com.web.spring.ideal_trip.model.enums.Rol;
import com.web.spring.ideal_trip.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /* ============ LECTURAS ============ */

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public List<Usuario> listarActivos() {
        return usuarioRepository.findByActivoTrue();
    }

    public List<Usuario> listarPorRol(Rol rol) {
        return usuarioRepository.findByRol(rol);
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado con email: " + email));
    }

    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    /* ============ ESCRITURAS ============ */

    /**
     * Registro de cliente. Cifra la contraseña, asigna rol CLIENTE
     * y deja activo=true. Lanza IllegalArgumentException si el email ya existe.
     */
    @Transactional
    public Usuario registrar(Usuario usuario) {
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new IllegalArgumentException(
                    "Ya existe un usuario con el email: " + usuario.getEmail());
        }
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setRol(Rol.CLIENTE);
        usuario.setActivo(true);
        log.info("Registrando usuario: {}", usuario.getEmail());
        return usuarioRepository.save(usuario);
    }

    /**
     * Crea un usuario administrativo. Pensado para uso interno
     * (CommandLineRunner de seed o panel de admin).
     */
    @Transactional
    public Usuario crearAdmin(Usuario usuario) {
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new IllegalArgumentException(
                    "Ya existe un usuario con el email: " + usuario.getEmail());
        }
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setRol(Rol.ADMIN);
        usuario.setActivo(true);
        log.info("Creando admin: {}", usuario.getEmail());
        return usuarioRepository.save(usuario);
    }

    /**
     * Actualiza datos de perfil. NO toca password ni rol ni activo.
     * Para password usar cambiarPassword().
     */
    @Transactional
    public Usuario actualizarPerfil(Long id, Usuario datos) {
        Usuario usuario = buscarPorId(id);
        usuario.setNombre(datos.getNombre());
        usuario.setApellido(datos.getApellido());
        usuario.setTelefono(datos.getTelefono());
        // El email solo se cambia si es distinto y no está tomado por otro.
        if (!usuario.getEmail().equalsIgnoreCase(datos.getEmail())) {
            if (usuarioRepository.existsByEmail(datos.getEmail())) {
                throw new IllegalArgumentException(
                        "El email " + datos.getEmail() + " ya está registrado");
            }
            usuario.setEmail(datos.getEmail());
        }
        log.info("Perfil actualizado: id={}", id);
        return usuario; // dirty checking
    }

    @Transactional
    public void cambiarPassword(Long id, String passwordActual, String passwordNuevo) {
        Usuario usuario = buscarPorId(id);
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        }
        usuario.setPassword(passwordEncoder.encode(passwordNuevo));
        log.info("Password cambiada para usuario id={}", id);
    }

    @Transactional
    public void desactivar(Long id) {
        Usuario usuario = buscarPorId(id);
        usuario.setActivo(false);
        log.info("Usuario desactivado: id={}, email={}", id, usuario.getEmail());
    }

    @Transactional
    public void activar(Long id) {
        Usuario usuario = buscarPorId(id);
        usuario.setActivo(true);
        log.info("Usuario activado: id={}, email={}", id, usuario.getEmail());
    }
}