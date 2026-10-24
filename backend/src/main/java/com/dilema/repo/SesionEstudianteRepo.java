package com.dilema.repo;

import com.dilema.modelo.SesionEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SesionEstudianteRepo extends JpaRepository<SesionEstudiante, SesionEstudiante.Clave> {
    boolean existsBySesionIdAndEstudianteId(Long sesionId, Long estudianteId);
    List<SesionEstudiante> findBySesionId(Long sesionId);
    void deleteBySesionIdAndEstudianteId(Long sesionId, Long estudianteId);
}
