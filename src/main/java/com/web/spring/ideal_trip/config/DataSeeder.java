package com.web.spring.ideal_trip.config;

import com.web.spring.ideal_trip.model.Destino;
import com.web.spring.ideal_trip.model.Paquete;
import com.web.spring.ideal_trip.model.Usuario;
import com.web.spring.ideal_trip.repository.DestinoRepository;
import com.web.spring.ideal_trip.repository.UsuarioRepository;
import com.web.spring.ideal_trip.service.PaqueteService;
import com.web.spring.ideal_trip.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Carga datos de ejemplo si la base está vacía.
 * Idempotente: si ya hay registros, no hace nada.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final DestinoRepository destinoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final PaqueteService paqueteService;

    @Override
    public void run(String... args) {
        seedUsuarios();
        seedDestinosYPaquetes();
    }

    /* ============ USUARIOS ============ */

    private void seedUsuarios() {
        if (usuarioRepository.count() > 0) {
            log.info("Seed de usuarios omitido (ya existen registros).");
            return;
        }

        Usuario admin = Usuario.builder()
                .nombre("Admin")
                .apellido("Ideal-Trip")
                .email("admin@ideal-trip.com")
                .password("admin123")
                .telefono("3001234567")
                .build();
        usuarioService.crearAdmin(admin);

        Usuario cliente = Usuario.builder()
                .nombre("Carlos")
                .apellido("Pérez")
                .email("cliente@ideal-trip.com")
                .password("cliente123")
                .telefono("3009876543")
                .build();
        usuarioService.registrar(cliente);

        log.info("Seed de usuarios listo: admin@ideal-trip.com / admin123, " +
                "cliente@ideal-trip.com / cliente123");
    }

    /* ============ DESTINOS Y PAQUETES ============ */

    private void seedDestinosYPaquetes() {
        if (destinoRepository.count() > 0) {
            log.info("Seed de destinos/paquetes omitido (ya existen registros).");
            return;
        }

        // ---- Destinos ----
        Destino paris = destinoRepository.save(Destino.builder()
                .nombre("París")
                .pais("Francia")
                .continente("Europa")
                .descripcion("La ciudad de la luz, hogar de la Torre Eiffel, el Louvre y los Campos Elíseos. París combina historia, gastronomía y romanticismo como ninguna otra ciudad del mundo.")
                .imagenUrl("https://picsum.photos/seed/paris/1200/600")
                .precioBase(new BigDecimal("1200000.00"))
                .build());

        Destino saoPaulo = destinoRepository.save(Destino.builder()
                .nombre("São Paulo")
                .pais("Brasil")
                .continente("América")
                .descripcion("La megaciudad más grande de Sudamérica, vibrante centro cultural y gastronómico de Brasil. Mercados, museos y vida nocturna sin igual.")
                .imagenUrl("https://picsum.photos/seed/saopaulo/1200/600")
                .precioBase(new BigDecimal("1500000.00"))
                .build());

        Destino santaMarta = destinoRepository.save(Destino.builder()
                .nombre("Santa Marta")
                .pais("Colombia")
                .continente("América")
                .descripcion("Joya del Caribe colombiano. Playas de arena blanca, la Sierra Nevada y el Parque Tayrona. Naturaleza, cultura indígena e historia en un mismo destino.")
                .imagenUrl("https://picsum.photos/seed/santamarta/1200/600")
                .precioBase(new BigDecimal("800000.00"))
                .build());

        Destino madrid = destinoRepository.save(Destino.builder()
                .nombre("Madrid")
                .pais("España")
                .continente("Europa")
                .descripcion("Capital española, cuna del arte, las tapas y la movida nocturna. Tres pinacotecas de clase mundial: Prado, Reina Sofía y Thyssen.")
                .imagenUrl("https://picsum.photos/seed/madrid/1200/600")
                .precioBase(new BigDecimal("1800000.00"))
                .build());

        Destino ciudadMexico = destinoRepository.save(Destino.builder()
                .nombre("Ciudad de México")
                .pais("México")
                .continente("América")
                .descripcion("Una de las metrópolis más grandes y vibrantes del mundo. Historia milenaria, gastronomía Patrimonio de la Humanidad y la mezcla perfecta entre lo prehispánico y lo moderno.")
                .imagenUrl("https://picsum.photos/seed/mexico/1200/600")
                .precioBase(new BigDecimal("1100000.00"))
                .build());

        log.info("Seed: 5 destinos creados.");

        // ---- Paquetes ----
        paqueteService.crear(Paquete.builder()
                .nombre("París Romántico")
                .tipo("Luna de Miel")
                .descripcion("Una semana en la ciudad del amor para parejas que buscan magia y sofisticación.")
                .incluye("Vuelos internacionales · Hotel 4 estrellas céntrico · Desayuno · Traslados aeropuerto · Tour Torre Eiffel · Crucero por el Sena")
                .precio(new BigDecimal("4500000.00"))
                .duracionDias(7)
                .cuposDisponibles(10)
                .build(), paris.getId());

        paqueteService.crear(Paquete.builder()
                .nombre("París Express")
                .tipo("Todo Incluido")
                .descripcion("Fin de semana largo en París para quienes tienen poco tiempo pero ganas de mucho.")
                .incluye("Vuelos · Hotel 3 estrellas · Tour por los principales monumentos · Museo del Louvre")
                .precio(new BigDecimal("2800000.00"))
                .duracionDias(4)
                .cuposDisponibles(15)
                .build(), paris.getId());

        paqueteService.crear(Paquete.builder()
                .nombre("Aventura en São Paulo")
                .tipo("Cultural")
                .descripcion("Sumérgete en la energía paulista: museos, mercados, gastronomía y arquitectura.")
                .incluye("Vuelos · Hotel 4 estrellas Avenida Paulista · Tour MASP · Mercado Municipal · City Tour")
                .precio(new BigDecimal("3200000.00"))
                .duracionDias(5)
                .cuposDisponibles(12)
                .build(), saoPaulo.getId());

        paqueteService.crear(Paquete.builder()
                .nombre("Caribe Santa Marta")
                .tipo("Familiar")
                .descripcion("Vacaciones en familia frente al mar Caribe y al pie de la Sierra Nevada.")
                .incluye("Vuelos · Hotel todo incluido · Tour Parque Tayrona · Snorkel en El Rodadero · Tour gastronómico")
                .precio(new BigDecimal("1900000.00"))
                .duracionDias(6)
                .cuposDisponibles(20)
                .build(), santaMarta.getId());

        paqueteService.crear(Paquete.builder()
                .nombre("Madrid Cultural")
                .tipo("Cultural")
                .descripcion("Recorre la capital española y sus tesoros artísticos.")
                .incluye("Vuelos · Hotel 4 estrellas centro · Entradas Prado y Reina Sofía · Palacio Real · Ruta de tapas")
                .precio(new BigDecimal("3800000.00"))
                .duracionDias(6)
                .cuposDisponibles(10)
                .build(), madrid.getId());

        paqueteService.crear(Paquete.builder()
                .nombre("CDMX Imperdible")
                .tipo("Todo Incluido")
                .descripcion("La esencia de México: historia, sabor y color en una semana.")
                .incluye("Vuelos · Hotel 4 estrellas Polanco · Tour Teotihuacán · Museo de Antropología · Xochimilco")
                .precio(new BigDecimal("2500000.00"))
                .duracionDias(7)
                .cuposDisponibles(15)
                .build(), ciudadMexico.getId());

        paqueteService.crear(Paquete.builder()
                .nombre("Aventura Tayrona")
                .tipo("Aventura")
                .descripcion("Para mochileros y amantes de la naturaleza pura. Senderismo, playas y noches bajo las estrellas.")
                .incluye("Vuelos · Hostal ecológico · Caminata al Cabo San Juan · Avistamiento de fauna · Equipo de campamento")
                .precio(new BigDecimal("1200000.00"))
                .duracionDias(4)
                .cuposDisponibles(8)
                .build(), santaMarta.getId());

        log.info("Seed: 7 paquetes creados.");
    }
}