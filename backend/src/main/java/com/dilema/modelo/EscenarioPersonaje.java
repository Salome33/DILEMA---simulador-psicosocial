package com.dilema.modelo;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "escenario_personaje")
@IdClass(EscenarioPersonaje.Clave.class)
public class EscenarioPersonaje {
    @Id
    @Column(name = "escenario_id")
    public Long escenarioId;
    @Id
    @Column(name = "personaje_id")
    public Long personajeId;
    @Column(name = "caso_id", nullable = false)
    public Long casoId;

    public static class Clave implements Serializable {
        public Long escenarioId;
        public Long personajeId;
        public Clave() {}
        public Clave(Long escenarioId, Long personajeId) {
            this.escenarioId = escenarioId;
            this.personajeId = personajeId;
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Clave c)) return false;
            return escenarioId.equals(c.escenarioId) && personajeId.equals(c.personajeId);
        }
        @Override public int hashCode() { return escenarioId.hashCode() * 31 + personajeId.hashCode(); }
    }
}
