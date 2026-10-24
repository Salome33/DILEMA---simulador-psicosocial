package com.dilema.repo;

import com.dilema.modelo.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GrupoRepo extends JpaRepository<Grupo, Long> {
    List<Grupo> findByDocenteId(Long docenteId);
}
