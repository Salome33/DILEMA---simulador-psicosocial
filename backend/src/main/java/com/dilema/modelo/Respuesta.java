package com.dilema.modelo;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "respuesta")
@IdClass(Respuesta.Clave.class)
public class Respuesta {
    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "participacion_id")
    public Participacion participacion;
    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "pregunta_id")
    public Pregunta pregunta;
    @Column(name = "caso_id", nullable = false)
    public Long casoId;
    @ManyToOne(optional = false)
    @JoinColumn(name = "opcion_id")
    public Opcion opcion;

    public static class Clave implements Serializable {
        public Long participacion;
        public Long pregunta;
        public Clave() {}
        @Override public boolean equals(Object o) {
            if (!(o instanceof Clave c)) return false;
            return participacion.equals(c.participacion) && pregunta.equals(c.pregunta);
        }
        @Override public int hashCode() { return participacion.hashCode() * 31 + pregunta.hashCode(); }
    }
}
