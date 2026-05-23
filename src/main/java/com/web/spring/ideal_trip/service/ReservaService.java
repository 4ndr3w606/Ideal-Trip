package com.web.spring.ideal_trip.service;

import com.web.spring.ideal_trip.exception.RecursoNoEncontradoException;
import com.web.spring.ideal_trip.model.Paquete;
import com.web.spring.ideal_trip.model.Reserva;
import com.web.spring.ideal_trip.model.Usuario;
import com.web.spring.ideal_trip.model.enums.EstadoReserva;
import com.web.spring.ideal_trip.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final UsuarioService usuarioService;
    private final PaqueteService paqueteService;

    /* ============ LECTURAS ============ */

    public List<Reserva> listarTodas() {
        return reservaRepository.findAll();
    }

    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", id));
    }

    public List<Reserva> listarPorUsuario(Long usuarioId) {
        return reservaRepository.findByUsuarioId(usuarioId);
    }

    public List<Reserva> listarPorPaquete(Long paqueteId) {
        return reservaRepository.findByPaqueteId(paqueteId);
    }

    public List<Reserva> listarPorEstado(EstadoReserva estado) {
        return reservaRepository.findByEstado(estado);
    }

    public List<Reserva> listarPorUsuarioYEstado(Long usuarioId, EstadoReserva estado) {
        return reservaRepository.findByUsuarioIdAndEstado(usuarioId, estado);
    }

    /* ============ ESCRITURAS ============ */

    /**
     * Crea una reserva en estado PENDIENTE, descuenta los cupos del paquete
     * y calcula el precioTotal a partir del precio del paquete y la cantidad
     * de personas. El parámetro 'datos' aporta fechaViaje, cantidadPersonas
     * y descripcion.
     */
    @Transactional
    public Reserva crear(Long usuarioId, Long paqueteId, Reserva datos) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        Paquete paquete = paqueteService.buscarPorId(paqueteId);

        validarFechaViaje(datos.getFechaViaje());

        int personas = datos.getCantidadPersonas();
        if (personas < 1) {
            throw new IllegalArgumentException(
                    "La cantidad de personas debe ser al menos 1");
        }

        // Descuenta cupos (lanza IllegalStateException si no alcanza).
        // Se hace ANTES del save: si falla, no se crea la reserva.
        paqueteService.descontarCupos(paqueteId, personas);

        BigDecimal precioTotal = paquete.getPrecio()
                .multiply(BigDecimal.valueOf(personas));

        Reserva reserva = Reserva.builder()
                .usuario(usuario)
                .paquete(paquete)
                .fechaViaje(datos.getFechaViaje())
                .cantidadPersonas(personas)
                .precioTotal(precioTotal)
                .estado(EstadoReserva.PENDIENTE)
                .descripcion(datos.getDescripcion())
                .build();

        log.info("Creando reserva: usuario={}, paquete={}, personas={}, total={}",
                usuario.getEmail(), paquete.getNombre(), personas, precioTotal);

        return reservaRepository.save(reserva);
    }

    /**
     * Transición de estado centralizada con sus reglas:
     *   - No se puede modificar una reserva COMPLETADA.
     *   - No se puede reactivar una reserva CANCELADA.
     *   - Si se cancela desde PENDIENTE o CONFIRMADA, se devuelven los cupos.
     */
    @Transactional
    public Reserva cambiarEstado(Long reservaId, EstadoReserva nuevoEstado) {
        Reserva reserva = buscarPorId(reservaId);
        EstadoReserva anterior = reserva.getEstado();

        if (anterior == nuevoEstado) {
            return reserva; // nada que hacer
        }
        if (anterior == EstadoReserva.COMPLETADA) {
            throw new IllegalStateException(
                    "No se puede modificar una reserva ya COMPLETADA");
        }
        if (anterior == EstadoReserva.CANCELADA) {
            throw new IllegalStateException(
                    "No se puede reactivar una reserva CANCELADA");
        }

        // Si se cancela una reserva que aún tenía los cupos retenidos, devolverlos.
        if (nuevoEstado == EstadoReserva.CANCELADA
                && (anterior == EstadoReserva.PENDIENTE
                || anterior == EstadoReserva.CONFIRMADA)) {
            paqueteService.liberarCupos(
                    reserva.getPaquete().getId(),
                    reserva.getCantidadPersonas());
        }

        reserva.setEstado(nuevoEstado);
        log.info("Reserva id={} cambió de {} a {}",
                reservaId, anterior, nuevoEstado);
        return reserva; // dirty checking
    }

    /** Atajos útiles para los controladores. */
    @Transactional
    public Reserva confirmar(Long reservaId) {
        return cambiarEstado(reservaId, EstadoReserva.CONFIRMADA);
    }

    @Transactional
    public Reserva cancelar(Long reservaId) {
        return cambiarEstado(reservaId, EstadoReserva.CANCELADA);
    }

    @Transactional
    public Reserva completar(Long reservaId) {
        return cambiarEstado(reservaId, EstadoReserva.COMPLETADA);
    }

    /* ============ HELPERS PRIVADOS ============ */

    private void validarFechaViaje(LocalDateTime fechaViaje) {
        if (fechaViaje == null) {
            throw new IllegalArgumentException(
                    "La fecha de viaje es obligatoria");
        }
        if (!fechaViaje.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "La fecha de viaje debe ser posterior al momento actual");
        }
    }
}