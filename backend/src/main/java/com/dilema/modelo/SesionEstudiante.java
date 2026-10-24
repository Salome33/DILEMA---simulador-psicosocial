package com.dilema.modelo;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "sesion_estudiante")
@IdClass(SesionEstudiante.Clave.class)
public class SesionEstudiante {
    @Id
    @Column(name = "sesion_id")
    public Long sesionId;
    @Id
    @Column(name = "estudiante_id")
    public Long estudianteId;
    @Column(name = "grupo_id", nullable = false)
    public Long grupoId;

    public static class Clave implements Serializable {
        public Long sesionId;
        public Long estudianteId;
        public Clave() {}
        @Override public boolean equals(Object o) {
            if (!(o instanceof Clave c)) return false;
            return sesionId.equals(c.sesionId) && estudianteId.equals(c.estudianteId);
        }
        @Override public int hashCode() { return sesionId.hashCode() * 31 + estudianteId.hashCode(); }
    }
}
