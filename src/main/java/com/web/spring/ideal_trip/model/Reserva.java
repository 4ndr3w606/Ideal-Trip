package com.web.spring.ideal_trip.model;

import com.web.spring.ideal_trip.model.enums.EstadoReserva;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"usuario", "paquete"})
@EqualsAndHashCode(of = "id")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_reserva_usuario"))
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="paquete_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reserva_paquete"))
    private Paquete paquete;

    @Column(name="fecha_reserva", nullable = false, updatable = false)
    private LocalDateTime fechaReserva;

    @NotNull
    @Future
    @Column(name ="fecha_viaje", nullable = false)
    private LocalDateTime fechaViaje;

    @Min(1)
    @Column(name="cantidad_personas", nullable = false)
    private int cantidadPersonas;

    @NotNull
    @DecimalMin(value= "0.0", inclusive = false)
    @Column(name ="precio_total", nullable = false, precision = 12, scale= 2)
    private BigDecimal precioTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoReserva estado = EstadoReserva.PENDIENTE;

    @Size(max=500)
    @Column(length = 500)
    private String descripcion;

    @PrePersist
    public void onCreate() {
        this.fechaReserva = LocalDateTime.now();
    }




}
