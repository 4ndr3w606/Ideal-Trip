
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/destinos")
@RequiredArgsConstructor
public class DestinoController {

    private final DestinoService destinoService;
    private final PaqueteService paqueteService;

    /**
     * GET /destinos          → todos los activos
     * GET /destinos?q=Paris  → búsqueda por nombre
     * GET /destinos?pais=Brasil → filtra por país
     * Si se pasan ambos, gana la búsqueda por nombre.
     */
    @GetMapping
    public String listar(
            @RequestParam(value = "q", required = false) String busqueda,
            @RequestParam(value = "pais", required = false) String pais,
            Model model) {

        List<Destino> destinos;
        if (busqueda != null && !busqueda.isBlank()) {
            destinos = destinoService.buscarPorNombre(busqueda);
        } else if (pais != null && !pais.isBlank()) {
            destinos = destinoService.buscarPorPais(pais);
        } else {
            destinos = destinoService.listarActivos();
        }

        Map<Long, Long> conteoPaquetes = new HashMap<>();
        for (Destino d : destinos) {
            conteoPaquetes.put(d.getId(), paqueteService.contarPorDestino(d.getId()));
        }

        model.addAttribute("destinos", destinos);
        model.addAttribute("conteoPaquetes", conteoPaquetes);
        model.addAttribute("busqueda", busqueda);
        model.addAttribute("paisSeleccionado", pais);

        return "destinos";
    }

    /** GET /destinos/{id} → detalle del destino con sus paquetes. */
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Destino destino = destinoService.buscarPorId(id);
        List<Paquete> paquetes = paqueteService.listarPorDestino(id);

        model.addAttribute("destino", destino);
        model.addAttribute("paquetes", paquetes);

        return "destino-detalle";
    }
}