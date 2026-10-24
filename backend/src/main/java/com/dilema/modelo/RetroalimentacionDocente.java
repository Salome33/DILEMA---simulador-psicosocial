package com.dilema.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "retroalimentacion_docente")
public class RetroalimentacionDocente {
    @Id
    @Column(name = "participacion_id")
    public Long id;
    @OneToOne
    @MapsId
    @JoinColumn(name = "participacion_id")
    public Participacion participacion;
    @Column(nullable = false, columnDefinition = "text")
    public String comentario;
}
