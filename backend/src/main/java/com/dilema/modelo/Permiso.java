package com.dilema.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "permiso")
public class Permiso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 40)
    public String tipo;
}
