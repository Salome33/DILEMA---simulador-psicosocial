package com.dilema.modelo;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "escenario", uniqueConstraints = @UniqueConstraint(columnNames = {"caso_id", "orden"}))
public class Escenario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "caso_id")
    public Caso caso;
    @Column(nullable = false)
    public int orden;
    @Column(nullable = false, length = 200)
    public String titulo;
    @Column(nullable = false, columnDefinition = "text")
    public String contexto;

    @OneToMany(mappedBy = "escenario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    public List<Pregunta> preguntas = new ArrayList<>();
}
