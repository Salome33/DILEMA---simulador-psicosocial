package com.dilema.modelo;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "grupo")
public class Grupo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "profesor_id")
    public Usuario docente;
    @Column(nullable = false, length = 150)
    public String nombre;
    @Column(nullable = false, length = 10)
    public String semestre;

    @ManyToMany
    @JoinTable(name = "estudiante_grupo",
            joinColumns = @JoinColumn(name = "grupo_id"),
            inverseJoinColumns = @JoinColumn(name = "estudiante_id"))
    public Set<Usuario> estudiantes = new HashSet<>();

    public boolean esDirigidoPor(Usuario profesor) {
        return docente != null && profesor != null && docente.id.equals(profesor.id);
    }

    public boolean pertenece(Usuario estudiante) {
        return estudiantes.stream().anyMatch(e -> e.id.equals(estudiante.id));
    }
}
