package com.dilema.repo;

import com.dilema.modelo.EscenarioPersonaje;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EscenarioPersonajeRepo extends JpaRepository<EscenarioPersonaje, EscenarioPersonaje.Clave> {
    List<EscenarioPersonaje> findByCasoId(Long casoId);
    void deleteByCasoId(Long casoId);
}
