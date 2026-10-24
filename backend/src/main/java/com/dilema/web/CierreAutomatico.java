package com.dilema.web;

import com.dilema.servicio.AcademiaServicio;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CierreAutomatico {
    private final AcademiaServicio academia;

    public CierreAutomatico(AcademiaServicio academia) {
        this.academia = academia;
    }

    @Scheduled(fixedDelay = 60000)
    public void cerrarSesionesVencidas() {
        academia.cerrarVencidas();
    }
}
