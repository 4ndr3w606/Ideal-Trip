package com.web.spring.ideal_trip.repository;

import com.web.spring.ideal_trip.model.Paquete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaqueteRepository extends JpaRepository<Paquete, Long> {

    List<Paquete> findByActivoTrue();

    List<Paquete> findByDestinoId(Long destinoId);

    List<Paquete> findByTipoIgnoreCase(String tipo);

    List<Paquete> findByPrecioBetween(BigDecimal min, BigDecimal max);

    long countByDestinoId(Long destinoId);
}