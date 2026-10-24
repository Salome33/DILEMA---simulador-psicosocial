package com.dilema.web;

import com.dilema.servicio.AccesoDenegado;
import com.dilema.servicio.NoEncontrado;
import com.dilema.servicio.ReglaNegocio;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ErroresApi {
    @ExceptionHandler(ReglaNegocio.class)
    ResponseEntity<Map<String, String>> regla(ReglaNegocio error) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage()));
    }

    @ExceptionHandler(AccesoDenegado.class)
    ResponseEntity<Map<String, String>> acceso(AccesoDenegado error) {
        return ResponseEntity.status(403).body(Map.of("mensaje", error.getMessage()));
    }

    @ExceptionHandler(NoEncontrado.class)
    ResponseEntity<Map<String, String>> noEncontrado(NoEncontrado error) {
        return ResponseEntity.status(404).body(Map.of("mensaje", error.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> argumento(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "El dato enviado no es válido."));
    }
}
