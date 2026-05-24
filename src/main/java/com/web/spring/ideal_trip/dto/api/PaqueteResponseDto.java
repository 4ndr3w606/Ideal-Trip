package com.web.spring.ideal_trip.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Schema(description = "Paquete turístico expuesto en el catálogo público")
public class PaqueteResponseDto {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "París Romántico")
    private String nombre;

    @Schema(example = "Luna de Miel",
            allowableValues = {"Todo Incluido", "Aventura", "Cultural", "Luna de Miel", "Familiar"})
    private String tipo;

    @Schema(example = "Luna de miel inolvidable en París con tour del Sena")
    private String descripcion;

    @Schema(example = "Vuelo · Hotel 5 estrellas · Traslados · Desayunos")
    private String incluye;

    @Schema(description = "Precio en pesos colombianos", example = "1500000.00")
    private BigDecimal precio;

    @Schema(example = "7")
    private int duracionDias;

    @Schema(description = "Cupos disponibles para reservar", example = "10")
    private int cuposDisponibles;

    @Schema(example = "true")
    private boolean activo;

    @Schema(description = "URL de la imagen del paquete (puede ser null)",
            example = "https://picsum.photos/seed/paris/800/450", nullable = true)
    private String imagenUrl;

    // Destino aplanado (evita ciclos y respeta "no exponer entidades JPA")
    @Schema(description = "Id del destino al que pertenece", example = "1")
    private Long destinoId;

    @Schema(example = "París")
    private String destinoNombre;

    @Schema(example = "Francia")
    private String destinoPais;
}