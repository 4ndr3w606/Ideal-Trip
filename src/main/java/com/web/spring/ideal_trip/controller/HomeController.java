package com.web.spring.ideal_trip.controller;

import com.web.spring.ideal_trip.model.Destino;
import com.web.spring.ideal_trip.model.Paquete;
import com.web.spring.ideal_trip.service.DestinoService;
import com.web.spring.ideal_trip.service.PaqueteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DestinoService destinoService;
    private final PaqueteService paqueteService;

    @GetMapping("/")
    public String inicio(Model model) {
        // Destinos destacados: hasta 3 activos
        List<Destino> destinos = destinoService.listarActivos()
                .stream()
                .limit(3)
                .toList();

        // Conteo de paquetes por destino, para el badge "X paquetes" del card
        Map<Long, Long> conteoPaquetes = new HashMap<>();
        for (Destino d : destinos) {
            conteoPaquetes.put(d.getId(), paqueteService.contarPorDestino(d.getId()));
        }

        // Paquetes destacados: hasta 2 activos
        List<Paquete> paquetes = paqueteService.listarActivos()
                .stream()
                .limit(2)
                .toList();

        model.addAttribute("destinos", destinos);
        model.addAttribute("paquetes", paquetes);
        model.addAttribute("conteoPaquetes", conteoPaquetes);

        return "index"; // se resuelve a templates/index.html
    }
}