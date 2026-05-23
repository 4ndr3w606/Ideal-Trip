package com.web.spring.ideal_trip.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String manejarRecursoNoEncontrado(RecursoNoEncontradoException ex, Model model) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String manejarIllegalArgument(IllegalArgumentException ex, Model model) {
        log.warn("Argumento invalido: {}", ex.getMessage());
        model.addAttribute("message", ex.getMessage());
        return "error/400";
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String manejarEstadoInvalido(IllegalStateException ex, Model model) {
        log.warn("Operación no permitida: {}", ex.getMessage());
        model.addAttribute("mensaje", ex.getMessage());
        return "error/409";
    }

    // 👇 Este es el handler nuevo (el que faltaba)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> manejarRecursoEstaticoNoEncontrado(NoResourceFoundException ex) {
        log.debug("Recurso estático no encontrado: {}", ex.getResourcePath());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String manejarErrorGeneral(Exception ex, Model model) {
        log.error("Error inesperado", ex);
        model.addAttribute("mensaje", "Ocurrió un error inesperado en el servidor.");
        return "error/500";
    }
}