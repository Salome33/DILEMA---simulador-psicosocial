package com.dilema.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "opcion")
public class Opcion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "pregunta_id")
    public Pregunta pregunta;
    @Column(nullable = false)
    public int orden;
    @Column(nullable = false, columnDefinition = "text")
    public String descripcion;
    @Column(name = "es_correcta", nullable = false)
    public boolean esCorrecta;

    @OneToOne(mappedBy = "opcion", cascade = CascadeType.ALL, orphanRemoval = true)
    public Retroalimentacion retroalimentacion;
}
