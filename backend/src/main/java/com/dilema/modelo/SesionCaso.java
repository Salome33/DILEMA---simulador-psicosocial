package com.dilema.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sesion")
public class SesionCaso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "caso_id")
    public Caso caso;
    @ManyToOne(optional = false)
    @JoinColumn(name = "grupo_id")
    public Grupo grupo;
    @Column(name = "inicio_programado", nullable = false)
    public LocalDateTime inicio;
    @Column(name = "fin_programado", nullable = false)
    public LocalDateTime fin;
    @Column(name = "tiempo_maximo_minutos", nullable = false)
    public int tiempoMaxMin;
    @Column(name = "limite_ingreso_minutos", nullable = false)
    public int toleranciaMin;
    @Column(length = 200)
    public String lugar;
    @Column(name = "mensaje_personalizado", columnDefinition = "text")
    public String mensaje;
    @Column(name = "codigo_autorizacion", nullable = false, unique = true, length = 20)
    public String codigo;
    @Column(name = "inicio_real")
    public LocalDateTime inicioReal;
    @Column(name = "cierre_real")
    public LocalDateTime cierreReal;

    public Catalogo.EstadoSesion estado() {
        if (cierreReal != null) return Catalogo.EstadoSesion.FINALIZADO;
        if (inicioReal != null) return Catalogo.EstadoSesion.EN_CURSO;
        return Catalogo.EstadoSesion.SIN_INICIAR;
    }

    public boolean permiteIngreso(LocalDateTime ahora) {
        if (estado() != Catalogo.EstadoSesion.EN_CURSO || inicioReal == null) return false;
        return !ahora.isAfter(inicioReal.plusMinutes(toleranciaMin));
    }
}
