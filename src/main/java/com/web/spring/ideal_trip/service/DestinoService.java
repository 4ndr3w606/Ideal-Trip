package com.web.spring.ideal_trip.service;

import com.web.spring.ideal_trip.exception.RecursoNoEncontradoException;
import com.web.spring.ideal_trip.model.Destino;
import com.web.spring.ideal_trip.repository.DestinoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DestinoService {

    private final DestinoRepository destinoRepository;

    /* ============ LECTURAS ============ */

    public List<Destino> listarTodos() {
        return destinoRepository.findAll();
    }

    public List<Destino> listarActivos() {
        return destinoRepository.findByActivoTrue();
    }

    public Destino buscarPorId(Long id) {
        return destinoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Destino", id));
    }

    public List<Destino> buscarPorPais(String pais) {
        return destinoRepository.findByPaisIgnoreCase(pais);
    }

    public List<Destino> buscarPorNombre(String texto) {
        return destinoRepository.findByNombreContainingIgnoreCase(texto);
    }

    public long contarActivos() {
        return destinoRepository.findByActivoTrue().size();
    }

    /* ============ ESCRITURAS ============ */

    @Transactional
    public Destino guardar(Destino destino) {
        log.info("Guardando destino: {}", destino.getNombre());
        return destinoRepository.save(destino);
    }

    @Transactional
    public Destino actualizar(Long id, Destino datos) {
        Destino destino = buscarPorId(id);
        destino.setNombre(datos.getNombre());
        destino.setPais(datos.getPais());
        destino.setContinente(datos.getContinente());
        destino.setDescripcion(datos.getDescripcion());
        destino.setImagenUrl(datos.getImagenUrl());
        destino.setPrecioBase(datos.getPrecioBase());
        log.info("Destino actualizado: id={}", id);
        return destino;     // gracias a @Transactional, JPA detecta el cambio
    }

    @Transactional
    public void desactivar(Long id) {
        Destino destino = buscarPorId(id);
        destino.setActivo(false);
        log.info("Destino desactivado: id={}, nombre={}", id, destino.getNombre());
    }

    @Transactional
    public void activar(Long id) {
        Destino destino = buscarPorId(id);
        destino.setActivo(true);
        log.info("Destino activado: id={}, nombre={}", id, destino.getNombre());
    }
}