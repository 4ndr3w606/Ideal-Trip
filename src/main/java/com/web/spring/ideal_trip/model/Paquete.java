package com.web.spring.ideal_trip.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "paquetes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"destino", "reservas"})
@EqualsAndHashCode(of = "id")
public class Paquete {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @NotBlank
   @Size(max =150)
   private String nombre;

   @NotBlank
    @Size(max=50)
    @Column(nullable = false, length=50)
    private String tipo;

   @Size(max = 1000)
    @Column(columnDefinition ="TEXT")
    private String descripcion;

    @Size(max = 255)
    @Column(name = "imagen_url", length = 255)
    private String imagenUrl;

   @Size(max =1000)
    @Column(columnDefinition="TEXT")
    private String incluye;

   @NotNull
    @DecimalMin(value = "0.0", inclusive=false)
   @Digits(integer = 10, fraction = 2)
   @Column(nullable = false, precision = 12, scale = 2)
   private BigDecimal precio;

    @Min(1)
    @Column(name = "duracion_dias", nullable = false)
    private int duracionDias;

    @Min(0)
    @Column(name = "cupos_disponibles", nullable = false)
    private int cuposDisponibles;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destino_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_paquete_destino"))
    private Destino destino;

    @OneToMany(mappedBy = "paquete", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Reserva> reservas = new ArrayList<>();


}
