package com.web.spring.ideal_trip.repository;

import com.web.spring.ideal_trip.model.Destino;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DestinoRepository extends JpaRepository<Destino, Long> {

    List<Destino> findByActivoTrue();

    List<Destino> findByPaisIgnoreCase(String pais);

    List<Destino> findByNombreContainingIgnoreCase(String nombre);
}