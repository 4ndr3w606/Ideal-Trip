package com.web.spring.ideal_trip.service;

import com.web.spring.ideal_trip.exception.RecursoNoEncontradoException;
import com.web.spring.ideal_trip.model.Destino;
import com.web.spring.ideal_trip.model.Paquete;
import com.web.spring.ideal_trip.repository.PaqueteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaqueteService {

    private final PaqueteRepository paqueteRepository;
    private final DestinoService destinoService;     // para validar destino en escrituras

    /* ============ LECTURAS ============ */

    public List<Paquete> listarTodos() {
        return paqueteRepository.findAll();
    }

    public List<Paquete> listarActivos() {
        return paqueteRepository.findByActivoTrue();
    }

    public Paquete buscarPorId(Long id) {
        return paqueteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paquete", id));
    }

    public List<Paquete> listarPorDestino(Long destinoId) {
        return paqueteRepository.findByDestinoId(destinoId);
    }

    public List<Paquete> listarPorTipo(String tipo) {
        return paqueteRepository.findByTipoIgnoreCase(tipo);
    }

    public List<Paquete> listarPorRangoPrecio(BigDecimal min, BigDecimal max) {
        return paqueteRepository.findByPrecioBetween(min, max);
    }

    public long contarPorDestino(Long destinoId) {
        return paqueteRepository.countByDestinoId(destinoId);
    }

    @Transactional
    public void liberarCupos(Long paqueteId, int cantidad) {
        Paquete paquete = buscarPorId(paqueteId);
        paquete.setCuposDisponibles(paquete.getCuposDisponibles() + cantidad);
        log.info("Liberados {} cupos del paquete id={}", cantidad, paqueteId);
    }

    /* ============ ESCRITURAS ============ */

    @Transactional
    public Paquete crear(Paquete paquete, Long destinoId) {
        Destino destino = destinoService.buscarPorId(destinoId);
        paquete.setDestino(destino);
        log.info("Creando paquete '{}' para destino '{}'",
                paquete.getNombre(), destino.getNombre());
        return paqueteRepository.save(paquete);
    }

    @Transactional
    public Paquete actualizar(Long id, Paquete datos) {
        Paquete paquete = buscarPorId(id);
        paquete.setNombre(datos.getNombre());
        paquete.setTipo(datos.getTipo());
        paquete.setDescripcion(datos.getDescripcion());
        paquete.setIncluye(datos.getIncluye());
        paquete.setImagenUrl(datos.getImagenUrl());     // ← nueva línea
        paquete.setPrecio(datos.getPrecio());
        paquete.setDuracionDias(datos.getDuracionDias());
        paquete.setCuposDisponibles(datos.getCuposDisponibles());
        log.info("Paquete actualizado: id={}", id);
        return paquete;
    }

    @Transactional
    public void desactivar(Long id) {
        Paquete paquete = buscarPorId(id);
        paquete.setActivo(false);
        log.info("Paquete desactivado: id={}", id);
    }

    @Transactional
    public void descontarCupos(Long paqueteId, int cantidad) {
        Paquete paquete = buscarPorId(paqueteId);
        if (paquete.getCuposDisponibles() < cantidad) {
            throw new IllegalStateException(
                    "No hay cupos suficientes en el paquete " + paquete.getNombre());
        }
        paquete.setCuposDisponibles(paquete.getCuposDisponibles() - cantidad);
        log.info("Descontados {} cupos del paquete id={}", cantidad, paqueteId);
    }

    @Transactional
    public void activar(Long id) {
        Paquete paquete = buscarPorId(id);
        paquete.setActivo(true);
        log.info("Paquete activado: id={}", id);
    }
}