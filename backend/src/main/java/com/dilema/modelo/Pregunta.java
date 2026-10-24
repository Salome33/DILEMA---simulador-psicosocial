package com.dilema.modelo;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pregunta")
public class Pregunta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "escenario_id")
    public Escenario escenario;
    @Column(name = "caso_id", nullable = false)
    public Long casoId;
    @Column(nullable = false)
    public int orden;
    @Column(nullable = false, columnDefinition = "text")
    public String enunciado;
    @Column(columnDefinition = "text")
    public String pista;

    @OneToMany(mappedBy = "pregunta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    public List<Opcion> opciones = new ArrayList<>();

    @OneToOne(mappedBy = "pregunta", cascade = CascadeType.ALL, orphanRemoval = true)
    public CriterioRubrica criterio;
}
