package com.web.spring.ideal_trip.dto.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Datos para crear una reserva nueva")
public class ReservaRequestDto {

    @NotNull(message = "El paquete es obligatorio")
    @Schema(description = "Id del paquete a reservar", example = "1")
    private Long paqueteId;

    @NotNull(message = "La fecha de viaje es obligatoria")
    @Future(message = "La fecha de viaje debe ser futura")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fecha y hora del viaje (formato ISO sin zona)",
            example = "2027-08-15T10:00:00")
    private LocalDateTime fechaViaje;

    @NotNull(message = "La cantidad de personas es obligatoria")
    @Min(value = 1, message = "Debe haber al menos 1 persona")
    @Schema(description = "Cantidad de personas (mínimo 1)", example = "2")
    private Integer cantidadPersonas;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    @Schema(description = "Notas opcionales del cliente",
            example = "Necesitamos asistencia con equipaje voluminoso", nullable = true)
    private String descripcion;
}