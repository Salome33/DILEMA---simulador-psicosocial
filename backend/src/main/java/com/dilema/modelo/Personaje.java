package com.dilema.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "personaje")
public class Personaje {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "caso_id")
    public Caso caso;
    @Column(nullable = false, length = 120)
    public String nombre;
    @Column(columnDefinition = "text")
    public String descripcion;
}
