package com.web.spring.ideal_trip.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class ReservaDto {

    @NotNull(message = "El paquete es obligatorio")
    private Long paqueteId;

    @NotNull(message = "La fecha de viaje es obligatoria")
    @Future(message = "La fecha de viaje debe ser futura")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fechaViaje;

    @NotNull(message = "La cantidad de personas es obligatoria")
    @Min(value = 1, message = "Debe haber al menos 1 persona")
    private Integer cantidadPersonas;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String descripcion;
}