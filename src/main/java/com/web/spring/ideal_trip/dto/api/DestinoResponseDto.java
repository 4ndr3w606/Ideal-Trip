package com.web.spring.ideal_trip.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Schema(description = "Destino turístico expuesto en el catálogo público")
public class DestinoResponseDto {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "Santa Marta")
    private String nombre;

    @Schema(example = "Colombia")
    private String pais;

    @Schema(example = "América")
    private String continente;

    @Schema(example = "Playas del Caribe colombiano")
    private String descripcion;

    @Schema(example = "https://picsum.photos/seed/santamarta/800/450")
    private String imagenUrl;

    @Schema(description = "Precio base en pesos colombianos (BigDecimal)", example = "800000.00")
    private BigDecimal precioBase;
}