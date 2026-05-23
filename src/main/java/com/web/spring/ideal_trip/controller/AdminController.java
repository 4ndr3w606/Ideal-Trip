package com.web.spring.ideal_trip.controller;

import com.web.spring.ideal_trip.dto.DestinoDto;
import com.web.spring.ideal_trip.dto.PaqueteDto;
import com.web.spring.ideal_trip.model.Destino;
import com.web.spring.ideal_trip.model.Paquete;
import com.web.spring.ideal_trip.repository.ReservaRepository;
import com.web.spring.ideal_trip.repository.UsuarioRepository;
import com.web.spring.ideal_trip.service.DestinoService;
import com.web.spring.ideal_trip.service.PaqueteService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.web.spring.ideal_trip.model.enums.EstadoReserva;
import com.web.spring.ideal_trip.service.ReservaService;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final DestinoService destinoService;
    private final PaqueteService paqueteService;
    private final ReservaService reservaService;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;

    /* ============================================================ */
    /*                          DASHBOARD                            */
    /* ============================================================ */

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalDestinos", destinoService.listarTodos().size());
        model.addAttribute("totalDestinosActivos", destinoService.listarActivos().size());
        model.addAttribute("totalPaquetes", paqueteService.listarTodos().size());
        model.addAttribute("totalPaquetesActivos", paqueteService.listarActivos().size());
        model.addAttribute("totalReservas", reservaRepository.count());
        model.addAttribute("totalUsuarios", usuarioRepository.count());
        return "admin/dashboard";
    }

    /* ============================================================ */
    /*                          DESTINOS                             */
    /* ============================================================ */

    @GetMapping("/destinos")
    public String listarDestinos(Model model) {
        model.addAttribute("destinos", destinoService.listarTodos());
        return "admin/destinos";
    }

    @GetMapping("/destinos/nuevo")
    public String formularioNuevoDestino(Model model) {
        model.addAttribute("destinoDto", new DestinoDto());
        model.addAttribute("modo", "crear");
        return "admin/destino-form";
    }

    @PostMapping("/destinos/nuevo")
    public String guardarNuevoDestino(
            @Valid @ModelAttribute("destinoDto") DestinoDto dto,
            BindingResult bindingResult,
            RedirectAttributes flash,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("modo", "crear");
            return "admin/destino-form";
        }

        Destino destino = Destino.builder()
                .nombre(dto.getNombre())
                .pais(dto.getPais())
                .continente(dto.getContinente())
                .descripcion(dto.getDescripcion())
                .imagenUrl(dto.getImagenUrl())
                .precioBase(dto.getPrecioBase())
                .build();

        destinoService.guardar(destino);
        flash.addFlashAttribute("mensajeExito", "Destino creado: " + destino.getNombre());
        return "redirect:/admin/destinos";
    }

    @GetMapping("/destinos/{id}/editar")
    public String formularioEditarDestino(@PathVariable Long id, Model model) {
        Destino destino = destinoService.buscarPorId(id);

        DestinoDto dto = new DestinoDto();
        dto.setNombre(destino.getNombre());
        dto.setPais(destino.getPais());
        dto.setContinente(destino.getContinente());
        dto.setDescripcion(destino.getDescripcion());
        dto.setImagenUrl(destino.getImagenUrl());
        dto.setPrecioBase(destino.getPrecioBase());

        model.addAttribute("destinoDto", dto);
        model.addAttribute("destinoId", id);
        model.addAttribute("modo", "editar");
        return "admin/destino-form";
    }

    @PostMapping("/destinos/{id}/editar")
    public String guardarDestinoEditado(
            @PathVariable Long id,
            @Valid @ModelAttribute("destinoDto") DestinoDto dto,
            BindingResult bindingResult,
            RedirectAttributes flash,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("destinoId", id);
            model.addAttribute("modo", "editar");
            return "admin/destino-form";
        }

        Destino datos = Destino.builder()
                .nombre(dto.getNombre())
                .pais(dto.getPais())
                .continente(dto.getContinente())
                .descripcion(dto.getDescripcion())
                .imagenUrl(dto.getImagenUrl())
                .precioBase(dto.getPrecioBase())
                .build();

        destinoService.actualizar(id, datos);
        flash.addFlashAttribute("mensajeExito", "Destino actualizado.");
        return "redirect:/admin/destinos";
    }

    @PostMapping("/destinos/{id}/desactivar")
    public String desactivarDestino(@PathVariable Long id, RedirectAttributes flash) {
        destinoService.desactivar(id);
        flash.addFlashAttribute("mensajeExito", "Destino desactivado.");
        return "redirect:/admin/destinos";
    }

    @PostMapping("/destinos/{id}/activar")
    public String activarDestino(@PathVariable Long id, RedirectAttributes flash) {
        destinoService.activar(id);
        flash.addFlashAttribute("mensajeExito", "Destino activado.");
        return "redirect:/admin/destinos";
    }

    /* ============================================================ */
    /*                          PAQUETES                             */
    /* ============================================================ */

    @GetMapping("/paquetes")
    public String listarPaquetes(Model model) {
        model.addAttribute("paquetes", paqueteService.listarTodos());
        return "admin/paquetes";
    }

    @GetMapping("/paquetes/nuevo")
    public String formularioNuevoPaquete(Model model) {
        model.addAttribute("paqueteDto", new PaqueteDto());
        model.addAttribute("destinos", destinoService.listarActivos());
        model.addAttribute("modo", "crear");
        return "admin/paquete-form";
    }

    @PostMapping("/paquetes/nuevo")
    public String guardarNuevoPaquete(
            @Valid @ModelAttribute("paqueteDto") PaqueteDto dto,
            BindingResult bindingResult,
            RedirectAttributes flash,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("destinos", destinoService.listarActivos());
            model.addAttribute("modo", "crear");
            return "admin/paquete-form";
        }

        Paquete paquete = Paquete.builder()
                .nombre(dto.getNombre())
                .tipo(dto.getTipo())
                .descripcion(dto.getDescripcion())
                .incluye(dto.getIncluye())
                .imagenUrl(dto.getImagenUrl())          // ← nueva línea
                .precio(dto.getPrecio())
                .duracionDias(dto.getDuracionDias())
                .cuposDisponibles(dto.getCuposDisponibles())
                .build();

        paqueteService.crear(paquete, dto.getDestinoId());
        flash.addFlashAttribute("mensajeExito", "Paquete creado: " + paquete.getNombre());
        return "redirect:/admin/paquetes";
    }

    @GetMapping("/paquetes/{id}/editar")
    public String formularioEditarPaquete(@PathVariable Long id, Model model) {
        Paquete paquete = paqueteService.buscarPorId(id);

        PaqueteDto dto = new PaqueteDto();
        dto.setNombre(paquete.getNombre());
        dto.setTipo(paquete.getTipo());
        dto.setDescripcion(paquete.getDescripcion());
        dto.setIncluye(paquete.getIncluye());
        dto.setImagenUrl(paquete.getImagenUrl());
        dto.setPrecio(paquete.getPrecio());
        dto.setDuracionDias(paquete.getDuracionDias());
        dto.setCuposDisponibles(paquete.getCuposDisponibles());
        dto.setDestinoId(paquete.getDestino().getId());

        model.addAttribute("paqueteDto", dto);
        model.addAttribute("destinos", destinoService.listarActivos());
        model.addAttribute("paqueteId", id);
        model.addAttribute("modo", "editar");
        return "admin/paquete-form";
    }

    @PostMapping("/paquetes/{id}/editar")
    public String guardarPaqueteEditado(
            @PathVariable Long id,
            @Valid @ModelAttribute("paqueteDto") PaqueteDto dto,
            BindingResult bindingResult,
            RedirectAttributes flash,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("destinos", destinoService.listarActivos());
            model.addAttribute("paqueteId", id);
            model.addAttribute("modo", "editar");
            return "admin/paquete-form";
        }

        Paquete datos = Paquete.builder()
                .nombre(dto.getNombre())
                .tipo(dto.getTipo())
                .descripcion(dto.getDescripcion())
                .incluye(dto.getIncluye())
                .imagenUrl(dto.getImagenUrl())
                .precio(dto.getPrecio())
                .duracionDias(dto.getDuracionDias())
                .cuposDisponibles(dto.getCuposDisponibles())
                .build();

        paqueteService.actualizar(id, datos);
        flash.addFlashAttribute("mensajeExito", "Paquete actualizado.");
        return "redirect:/admin/paquetes";
    }

    @PostMapping("/paquetes/{id}/desactivar")
    public String desactivarPaquete(@PathVariable Long id, RedirectAttributes flash) {
        paqueteService.desactivar(id);
        flash.addFlashAttribute("mensajeExito", "Paquete desactivado.");
        return "redirect:/admin/paquetes";
    }


    /* ============================================================ */
    /*                          RESERVAS                             */
    /* ============================================================ */

    @GetMapping("/reservas")
    public String listarReservas(
            @RequestParam(required = false) EstadoReserva estado,
            Model model) {


        var reservas = (estado != null)
                ? reservaService.listarPorEstado(estado)
                : reservaService.listarTodas();

        model.addAttribute("reservas", reservas);
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("estados", EstadoReserva.values());
        return "admin/reservas";
    }

    @PostMapping("/reservas/{id}/confirmar")
    public String confirmarReserva(@PathVariable Long id, RedirectAttributes flash) {
        try {
            reservaService.confirmar(id);
            flash.addFlashAttribute("mensajeExito",
                    "Reserva #" + id + " confirmada.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/admin/reservas";
    }

    @PostMapping("/reservas/{id}/completar")
    public String completarReserva(@PathVariable Long id, RedirectAttributes flash) {
        try {
            reservaService.completar(id);
            flash.addFlashAttribute("mensajeExito",
                    "Reserva #" + id + " marcada como completada.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/admin/reservas";
    }

    @PostMapping("/reservas/{id}/cancelar")
    public String cancelarReservaAdmin(@PathVariable Long id, RedirectAttributes flash) {
        try {
            reservaService.cancelar(id);
            flash.addFlashAttribute("mensajeExito",
                    "Reserva #" + id + " cancelada. Los cupos se liberaron.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/admin/reservas";
    }

}