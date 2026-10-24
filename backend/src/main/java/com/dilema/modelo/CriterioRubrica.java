package com.dilema.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "criterio_rubrica")
public class CriterioRubrica {
    @Id
    @Column(name = "pregunta_id")
    public Long id;
    @OneToOne
    @MapsId
    @JoinColumn(name = "pregunta_id")
    public Pregunta pregunta;
    @Column(nullable = false, length = 300)
    public String criterio;
    @Column(nullable = false, precision = 2, scale = 1)
    public BigDecimal peso;
}
