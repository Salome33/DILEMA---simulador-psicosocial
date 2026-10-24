package com.dilema.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "evaluacion")
public class Evaluacion {
    @Id
    @Column(name = "participacion_id")
    public Long id;
    @OneToOne
    @MapsId
    @JoinColumn(name = "participacion_id")
    public Participacion participacion;
    @Column(nullable = false, precision = 2, scale = 1)
    public BigDecimal puntaje;
}
