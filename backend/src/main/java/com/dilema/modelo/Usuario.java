package com.dilema.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, length = 120)
    public String nombre;
    @Column(nullable = false, unique = true, length = 254)
    public String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Catalogo.Rol rol;
    @Column(length = 255)
    public String contrasena;
    @Column(nullable = false)
    public boolean activo = true;
}
