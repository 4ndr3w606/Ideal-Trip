package com.web.spring.ideal_trip.controller;

import com.web.spring.ideal_trip.model.Destino;
import com.web.spring.ideal_trip.model.Paquete;
import com.web.spring.ideal_trip.service.DestinoService;
import com.web.spring.ideal_trip.service.PaqueteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/paquetes")
@RequiredArgsConstructor
public class PaqueteController {

    private final PaqueteService paqueteService;
    private final DestinoService destinoService;

    /**
     * Filtros excluyentes con prioridad: destinoId > tipo > rango precio > todos.
     * Si necesitas combinarlos en el futuro, hay que añadir un método al
     * repositorio (por ejemplo findByTipoAndPrecioBetween) o usar Specifications.
     */
    @GetMapping
    public String listar(
            @RequestParam(value = "destinoId", required = false) Long destinoId,
            @RequestParam(value = "tipo", required = false) String tipo,
            @RequestParam(value = "precioMin", required = false) BigDecimal precioMin,
            @RequestParam(value = "precioMax", required = false) BigDecimal precioMax,
            Model model) {

        List<Paquete> paquetes;
        if (destinoId != null) {
            paquetes = paqueteService.listarPorDestino(destinoId);
        } else if (tipo != null && !tipo.isBlank()) {
            paquetes = paqueteService.listarPorTipo(tipo);
        } else if (precioMin != null && precioMax != null) {
            paquetes = paqueteService.listarPorRangoPrecio(precioMin, precioMax);
        } else {
            paquetes = paqueteService.listarActivos();
        }

        // Para llenar el dropdown de destinos en el panel de filtros
        List<Destino> destinos = destinoService.listarActivos();

        model.addAttribute("paquetes", paquetes);
        model.addAttribute("destinos", destinos);
        model.addAttribute("destinoSeleccionado", destinoId);
        model.addAttribute("tipoSeleccionado", tipo);
        model.addAttribute("precioMin", precioMin);
        model.addAttribute("precioMax", precioMax);

        return "paquetes";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Paquete paquete = paqueteService.buscarPorId(id);
        model.addAttribute("paquete", paquete);
        return "paquete-detalle";
    }
}