package com.dilema.repo;

import com.dilema.modelo.SesionCaso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface SesionRepo extends JpaRepository<SesionCaso, Long> {
    Optional<SesionCaso> findByCodigoIgnoreCase(String codigo);
    List<SesionCaso> findByGrupo_Docente_IdOrderByIdDesc(Long docenteId);
    List<SesionCaso> findByCierreRealIsNull();

    @Query("select s from SesionCaso s, SesionEstudiante a where a.sesionId = s.id and a.estudianteId = :estudianteId order by s.id desc")
    List<SesionCaso> delEstudiante(Long estudianteId);
}
