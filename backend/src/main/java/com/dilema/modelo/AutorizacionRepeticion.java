package com.dilema.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "autorizacion_repeticion")
public class AutorizacionRepeticion {
    @Id
    @Column(name = "participacion_id")
    public Long id;
    @OneToOne
    @MapsId
    @JoinColumn(name = "participacion_id")
    public Participacion participacion;
    @Column(name = "fecha_autorizacion", nullable = false)
    public LocalDateTime fechaAutorizacion;
}
