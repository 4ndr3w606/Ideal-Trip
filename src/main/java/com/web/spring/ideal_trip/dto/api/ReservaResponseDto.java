package com.web.spring.ideal_trip.dto.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "Reserva tal como se devuelve al cliente")
public class ReservaResponseDto {

    @Schema(example = "100")
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(example = "2026-05-23T14:30:00")
    private LocalDateTime fechaReserva;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(example = "2027-08-15T10:00:00")
    private LocalDateTime fechaViaje;

    @Schema(example = "2")
    private int cantidadPersonas;

    @Schema(description = "Precio total en COP", example = "3000000.00")
    private BigDecimal precioTotal;

    @Schema(description = "Estado actual de la reserva",
            example = "PENDIENTE",
            allowableValues = {"PENDIENTE", "CONFIRMADA", "CANCELADA", "COMPLETADA"})
    private String estado;

    @Schema(example = "Necesitamos asistencia con equipaje voluminoso", nullable = true)
    private String descripcion;

    // Paquete aplanado
    @Schema(example = "1")
    private Long paqueteId;

    @Schema(example = "París Romántico")
    private String paqueteNombre;
}