package com.dilema.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "retroalimentacion")
public class Retroalimentacion {
    @Id
    @Column(name = "opcion_id")
    public Long id;
    @OneToOne
    @MapsId
    @JoinColumn(name = "opcion_id")
    public Opcion opcion;
    @Column(nullable = false, columnDefinition = "text")
    public String explicacion;
}
