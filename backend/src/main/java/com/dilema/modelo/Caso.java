package com.dilema.modelo;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "caso")
public class Caso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "autor_id")
    public Usuario docente;
    @Column(nullable = false, length = 200)
    public String nombre;
    @Column(name = "desarrollo_situacional", columnDefinition = "text")
    public String desarrolloSituacional;
    @Column(columnDefinition = "text")
    public String contexto;
    @Column(name = "tiempo_estimado_minutos", nullable = false)
    public Integer tiempoEstimadoMin;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Catalogo.EstadoCaso estado = Catalogo.EstadoCaso.BORRADOR;

    @OneToMany(mappedBy = "caso", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    public List<Escenario> escenarios = new ArrayList<>();

    @OneToMany(mappedBy = "caso", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Personaje> personajes = new ArrayList<>();
}
