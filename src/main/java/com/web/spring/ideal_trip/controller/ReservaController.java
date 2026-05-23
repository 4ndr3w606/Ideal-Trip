package com.web.spring.ideal_trip.controller;

import com.web.spring.ideal_trip.dto.ReservaDto;
import com.web.spring.ideal_trip.model.Paquete;
import com.web.spring.ideal_trip.model.Reserva;
import com.web.spring.ideal_trip.model.Usuario;
import com.web.spring.ideal_trip.service.PaqueteService;
import com.web.spring.ideal_trip.service.ReservaService;
import com.web.spring.ideal_trip.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.web.spring.ideal_trip.model.enums.EstadoReserva;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ReservaController {

    private final ReservaService reservaService;
    private final PaqueteService paqueteService;
    private final UsuarioService usuarioService;

    /** GET /reservas/nueva?paqueteId=X → formulario pre-llenado. */
    @GetMapping("/reservas/nueva")
    public String mostrarFormulario(
            @RequestParam("paqueteId") Long paqueteId,
            Model model) {

        Paquete paquete = paqueteService.buscarPorId(paqueteId);

        ReservaDto dto = new ReservaDto();
        dto.setPaqueteId(paqueteId);
        dto.setCantidadPersonas(1);

        model.addAttribute("reservaDto", dto);
        model.addAttribute("paquete", paquete);
        return "reserva-form";
    }

    /** POST /reservas → crea la reserva para el usuario logueado. */
    @PostMapping("/reservas")
    public String procesarReserva(
            @Valid @ModelAttribute("reservaDto") ReservaDto dto,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes flash) {

        Paquete paquete = paqueteService.buscarPorId(dto.getPaqueteId());

        // Validación adicional: cupos suficientes
        if (dto.getCantidadPersonas() != null
                && paquete.getCuposDisponibles() < dto.getCantidadPersonas()) {
            bindingResult.rejectValue("cantidadPersonas", "cupos.insuficientes",
                    "No hay cupos suficientes (disponibles: "
                            + paquete.getCuposDisponibles() + ")");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("paquete", paquete);
            return "reserva-form";
        }

        Usuario usuario = usuarioService.buscarPorEmail(principal.getName());

        Reserva datos = Reserva.builder()
                .fechaViaje(dto.getFechaViaje())
                .cantidadPersonas(dto.getCantidadPersonas())
                .descripcion(dto.getDescripcion())
                .build();

        Reserva creada = reservaService.crear(usuario.getId(), dto.getPaqueteId(), datos);

        log.info("Reserva {} creada por usuario {}", creada.getId(), usuario.getEmail());
        flash.addFlashAttribute("mensajeExito",
                "¡Reserva creada con éxito! Estado: PENDIENTE.");
        return "redirect:/mis-reservas";
    }

    /** GET /mis-reservas → lista las reservas del usuario logueado. */
    @GetMapping("/mis-reservas")
    public String listarMisReservas(Principal principal, Model model) {
        Usuario usuario = usuarioService.buscarPorEmail(principal.getName());
        List<Reserva> reservas = reservaService.listarPorUsuario(usuario.getId());
        model.addAttribute("reservas", reservas);
        return "mis-reservas";
    }

    /** POST /reservas/{id}/cancelar → cancela una reserva propia. */
    @PostMapping("/reservas/{id}/cancelar")
    public String cancelar(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes flash) {

        Reserva reserva = reservaService.buscarPorId(id);
        Usuario usuario = usuarioService.buscarPorEmail(principal.getName());

        // Seguridad: el cliente solo puede cancelar SUS reservas
        if (!reserva.getUsuario().getId().equals(usuario.getId())) {
            flash.addFlashAttribute("mensajeError",
                    "No tienes permiso para cancelar esta reserva.");
            return "redirect:/mis-reservas";
        }

        try {
            reservaService.cancelar(id);
            flash.addFlashAttribute("mensajeExito", "Reserva cancelada correctamente.");
        } catch (IllegalStateException ex) {
            // Por ejemplo: ya estaba CANCELADA o COMPLETADA
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }

        return "redirect:/mis-reservas";
    }

    /* ============================================================ */
    /*                       SIMULACIÓN DE PAGO                      */
    /* ============================================================ */

    @PostMapping("/reservas/{id}/pagar")
    public String procesarPago(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes flash) {

        Reserva reserva = reservaService.buscarPorId(id);
        Usuario usuario = usuarioService.buscarPorEmail(principal.getName());

        // Autorización a nivel de fila: solo el dueño puede pagar
        if (!reserva.getUsuario().getId().equals(usuario.getId())) {
            flash.addFlashAttribute("mensajeError",
                    "No tienes permiso para pagar esta reserva.");
            return "redirect:/mis-reservas";
        }

        // Solo se paga una reserva PENDIENTE
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            flash.addFlashAttribute("mensajeError",
                    "Esta reserva ya fue procesada (estado: " + reserva.getEstado() + ").");
            return "redirect:/mis-reservas";
        }

        // Pago "exitoso" — confirmar la reserva
        reservaService.confirmar(id);

        log.info("Pago simulado OK: reserva={} usuario={}", id, usuario.getEmail());

        flash.addFlashAttribute("mensajeExito",
                "Pago procesado correctamente. Tu reserva está CONFIRMADA.");
        return "redirect:/mis-reservas";
    }
}