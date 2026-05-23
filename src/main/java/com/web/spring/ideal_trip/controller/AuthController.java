package com.web.spring.ideal_trip.controller;

import com.web.spring.ideal_trip.dto.RegistroDto;
import com.web.spring.ideal_trip.model.Usuario;
import com.web.spring.ideal_trip.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;

    /** GET /login → muestra el formulario. El POST lo procesa Spring Security. */
    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    /** GET /registro → muestra el formulario vacío para crear cuenta. */
    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("registroDto", new RegistroDto());
        return "registro";
    }

    /** POST /registro → procesa la creación de la cuenta. */
    @PostMapping("/registro")
    public String procesarRegistro(
            @Valid @ModelAttribute("registroDto") RegistroDto dto,
            BindingResult bindingResult,
            RedirectAttributes flash) {

        // Validación cruzada: passwords iguales
        if (!dto.getPassword().equals(dto.getConfirmarPassword())) {
            bindingResult.rejectValue("confirmarPassword", "password.mismatch",
                    "Las contraseñas no coinciden");
        }

        // Validación de unicidad: email no usado
        if (usuarioService.existeEmail(dto.getEmail())) {
            bindingResult.rejectValue("email", "email.duplicate",
                    "Ya existe una cuenta con este email");
        }

        if (bindingResult.hasErrors()) {
            return "registro"; // vuelve al formulario con errores marcados
        }

        Usuario usuario = Usuario.builder()
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .email(dto.getEmail())
                .password(dto.getPassword())   // UsuarioService.registrar() lo hashea con BCrypt
                .telefono(dto.getTelefono())
                .build();

        usuarioService.registrar(usuario);

        flash.addFlashAttribute("mensajeExito",
                "¡Cuenta creada con éxito! Ya puedes iniciar sesión.");
        return "redirect:/login";
    }
}