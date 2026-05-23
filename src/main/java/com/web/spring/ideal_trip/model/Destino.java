package com.web.spring.ideal_trip.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "destinos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "paquetes")
@EqualsAndHashCode(of = "id")
public class Destino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;


    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String pais;

    @Size(max = 50)
    @Column(length = 50)
    private String continente;

    @Size(max = 2000)
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Size(max = 255)
    @Column(name = "imagen_url", length = 255)
    private String imagenUrl;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "precio_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBase;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @OneToMany(mappedBy = "destino", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Paquete> paquetes = new ArrayList<>();


}
