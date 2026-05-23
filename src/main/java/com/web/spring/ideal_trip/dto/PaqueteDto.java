package com.web.spring.ideal_trip.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaqueteDto {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150)
    private String nombre;

    @NotBlank(message = "El tipo es obligatorio")
    @Size(max = 50)
    private String tipo;

    @Size(max = 1000)
    private String descripcion;

    @Size(max = 1000)
    private String incluye;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal precio;

    @NotNull(message = "La duración es obligatoria")
    @Min(value = 1, message = "Mínimo 1 día")
    private Integer duracionDias;

    @NotNull(message = "Los cupos son obligatorios")
    @Min(value = 0, message = "No puede ser negativo")
    private Integer cuposDisponibles;

    @NotNull(message = "Debe seleccionar un destino")
    private Long destinoId;

    @Size(max = 255, message = "La URL no puede exceder 255 caracteres")
    private String imagenUrl;
}