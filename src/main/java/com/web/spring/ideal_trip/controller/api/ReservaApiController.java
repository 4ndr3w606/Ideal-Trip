package com.web.spring.ideal_trip.controller.api;

import com.web.spring.ideal_trip.dto.api.ReservaRequestDto;
import com.web.spring.ideal_trip.dto.api.ReservaResponseDto;
import com.web.spring.ideal_trip.model.Paquete;
import com.web.spring.ideal_trip.model.Reserva;
import com.web.spring.ideal_trip.model.Usuario;
import com.web.spring.ideal_trip.model.enums.EstadoReserva;
import com.web.spring.ideal_trip.service.PaqueteService;
import com.web.spring.ideal_trip.service.ReservaService;
import com.web.spring.ideal_trip.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reservas", description = "Gestión de reservas del cliente autenticado")
@SecurityRequirement(name = "bearerAuth")
public class ReservaApiController {

    private final ReservaService reservaService;
    private final PaqueteService paqueteService;
    private final UsuarioService usuarioService;

    @GetMapping
    @Operation(
            summary = "Listar mis reservas",
            description = "Devuelve todas las reservas del usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado devuelto correctamente"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido")
    })
    public List<ReservaResponseDto> misReservas(Principal principal) {
        Usuario usuario = usuarioService.buscarPorEmail(principal.getName());
        return reservaService.listarPorUsuario(usuario.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping
    @Operation(
            summary = "Crear una reserva nueva",
            description = """
                    Crea una reserva en estado PENDIENTE para el usuario autenticado.
                    Descuenta los cupos del paquete automáticamente. Para confirmarla,
                    el cliente debe llamar después a `POST /api/reservas/{id}/pagar`.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva creada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o cupos insuficientes"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "404", description = "El paquete no existe"),
            @ApiResponse(responseCode = "409", description = "Conflicto (sin cupos al intentar descontar)")
    })
    public ResponseEntity<ReservaResponseDto> crear(
            @Valid @RequestBody ReservaRequestDto request,
            Principal principal) {

        Usuario usuario = usuarioService.buscarPorEmail(principal.getName());

        // Validación temprana de cupos para devolver 400 con mensaje claro
        // (el servicio también valida y lanza IllegalStateException → 409 si hay race)
        Paquete paquete = paqueteService.buscarPorId(request.getPaqueteId());
        if (paquete.getCuposDisponibles() < request.getCantidadPersonas()) {
            throw new IllegalArgumentException(
                    "No hay cupos suficientes (disponibles: "
                            + paquete.getCuposDisponibles() + ")");
        }

        Reserva datos = Reserva.builder()
                .fechaViaje(request.getFechaViaje())
                .cantidadPersonas(request.getCantidadPersonas())
                .descripcion(request.getDescripcion())
                .build();

        Reserva creada = reservaService.crear(
                usuario.getId(), request.getPaqueteId(), datos);

        log.info("API: reserva {} creada por {}", creada.getId(), usuario.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(creada));
    }

    @PostMapping("/{id}/pagar")
    @Operation(
            summary = "Pagar una reserva propia",
            description = """
                    Simulación de pago: confirma la reserva sin validar tarjeta real.
                    Requiere que la reserva esté en estado PENDIENTE y sea del usuario
                    autenticado. Pasa a CONFIRMADA.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago procesado, reserva CONFIRMADA"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "La reserva no pertenece al usuario autenticado"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada"),
            @ApiResponse(responseCode = "409", description = "La reserva no está en estado PENDIENTE")
    })
    public ResponseEntity<?> pagar(
            @Parameter(description = "Id de la reserva a pagar", example = "100")
            @PathVariable Long id,
            Principal principal) {

        Reserva reserva = reservaService.buscarPorId(id);
        Usuario usuario = usuarioService.buscarPorEmail(principal.getName());

        if (!reserva.getUsuario().getId().equals(usuario.getId())) {
            log.warn("Usuario {} intentó pagar reserva ajena id={}", usuario.getEmail(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error", "Forbidden",
                            "mensaje", "No tienes permiso para pagar esta reserva"));
        }

        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "Conflict",
                            "mensaje", "La reserva ya fue procesada (estado: " + reserva.getEstado() + ")"));
        }

        Reserva confirmada = reservaService.confirmar(id);
        log.info("API: pago simulado OK reserva={} usuario={}", id, usuario.getEmail());

        return ResponseEntity.ok(toDto(confirmada));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(
            summary = "Cancelar una reserva propia",
            description = """
                    Cancela una reserva del usuario autenticado. Si estaba en PENDIENTE
                    o CONFIRMADA, libera los cupos al paquete. No se puede cancelar una
                    reserva CANCELADA o COMPLETADA.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva cancelada"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "La reserva no pertenece al usuario autenticado"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada"),
            @ApiResponse(responseCode = "409", description = "Estado no permite cancelación")
    })
    public ResponseEntity<?> cancelar(
            @Parameter(description = "Id de la reserva a cancelar", example = "100")
            @PathVariable Long id,
            Principal principal) {

        Reserva reserva = reservaService.buscarPorId(id);
        Usuario usuario = usuarioService.buscarPorEmail(principal.getName());

        if (!reserva.getUsuario().getId().equals(usuario.getId())) {
            log.warn("Usuario {} intentó cancelar reserva ajena id={}", usuario.getEmail(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error", "Forbidden",
                            "mensaje", "No tienes permiso para cancelar esta reserva"));
        }

        Reserva cancelada = reservaService.cancelar(id);
        return ResponseEntity.ok(toDto(cancelada));
    }

    /* ============ MAPPER ============ */

    private ReservaResponseDto toDto(Reserva r) {
        return new ReservaResponseDto(
                r.getId(),
                r.getFechaReserva(),
                r.getFechaViaje(),
                r.getCantidadPersonas(),
                r.getPrecioTotal(),
                r.getEstado().name(),
                r.getDescripcion(),
                r.getPaquete().getId(),
                r.getPaquete().getNombre()
        );
    }
}