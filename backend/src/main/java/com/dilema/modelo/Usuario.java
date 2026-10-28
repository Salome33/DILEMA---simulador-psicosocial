package com.dilema.modelo;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

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

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_permiso",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "permiso_id"))
    public Set<Permiso> permisos = new HashSet<>();

    public boolean tiene(String tipo) {
        return permisos.stream().anyMatch(permiso -> tipo.equals(permiso.tipo));
    }
}
