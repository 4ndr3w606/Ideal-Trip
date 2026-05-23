package com.web.spring.ideal_trip.repository;

import com.web.spring.ideal_trip.model.Reserva;
import com.web.spring.ideal_trip.model.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByUsuarioId(Long usuarioId);

    List<Reserva> findByPaqueteId(Long paqueteId);

    List<Reserva> findByEstado(EstadoReserva estado);

    List<Reserva> findByUsuarioIdAndEstado(Long usuarioId, EstadoReserva estado);
}