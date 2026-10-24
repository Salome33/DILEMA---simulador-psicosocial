package com.dilema.repo;

import com.dilema.modelo.Participacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParticipacionRepo extends JpaRepository<Participacion, Long> {
    List<Participacion> findByEstudianteIdAndSesionId(Long estudianteId, Long sesionId);
    List<Participacion> findByEstudianteIdOrderByIdDesc(Long estudianteId);
}
