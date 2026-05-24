package com.web.spring.ideal_trip.controller.api;

import com.web.spring.ideal_trip.dto.api.PaqueteResponseDto;
import com.web.spring.ideal_trip.model.Paquete;
import com.web.spring.ideal_trip.service.PaqueteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/paquetes")
@RequiredArgsConstructor
@Tag(name = "Paquetes", description = "Catálogo público de paquetes turísticos")
public class PaqueteApiController {

    private final PaqueteService paqueteService;

    @GetMapping
    @Operation(
            summary = "Listar paquetes activos",
            description = """
                    Devuelve todos los paquetes disponibles para reservar. Acepta filtros
                    opcionales por destino, tipo y rango de precio. Sin filtros, devuelve
                    todos los activos. No requiere autenticación.
                    """)
    @ApiResponse(responseCode = "200", description = "Listado devuelto correctamente")
    public List<PaqueteResponseDto> listar(
            @Parameter(description = "Id del destino para filtrar", example = "1")
            @RequestParam(required = false) Long destinoId,

            @Parameter(description = "Tipo del paquete (case-insensitive)", example = "Aventura")
            @RequestParam(required = false) String tipo,

            @Parameter(description = "Precio mínimo (inclusivo)", example = "500000")
            @RequestParam(required = false) BigDecimal precioMin,

            @Parameter(description = "Precio máximo (inclusivo)", example = "2000000")
            @RequestParam(required = false) BigDecimal precioMax) {

        List<Paquete> paquetes;

        if (destinoId != null) {
            paquetes = paqueteService.listarPorDestino(destinoId);
        } else if (tipo != null && !tipo.isBlank()) {
            paquetes = paqueteService.listarPorTipo(tipo);
        } else if (precioMin != null && precioMax != null) {
            paquetes = paqueteService.listarPorRangoPrecio(precioMin, precioMax);
        } else {
            paquetes = paqueteService.listarActivos();
        }

        return paquetes.stream()
                .filter(Paquete::isActivo)
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Detalle de un paquete",
            description = "Devuelve el paquete con el id indicado. No requiere autenticación.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paquete encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un paquete con ese id")
    })
    public PaqueteResponseDto detalle(
            @Parameter(description = "Id del paquete", example = "1")
            @PathVariable Long id) {
        return toDto(paqueteService.buscarPorId(id));
    }

    private PaqueteResponseDto toDto(Paquete p) {
        return new PaqueteResponseDto(
                p.getId(),
                p.getNombre(),
                p.getTipo(),
                p.getDescripcion(),
                p.getIncluye(),
                p.getPrecio(),
                p.getDuracionDias(),
                p.getCuposDisponibles(),
                p.isActivo(),
                p.getImagenUrl(),
                p.getDestino().getId(),
                p.getDestino().getNombre(),
                p.getDestino().getPais()
        );
    }
}