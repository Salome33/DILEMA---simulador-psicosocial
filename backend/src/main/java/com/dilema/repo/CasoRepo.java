package com.dilema.repo;

import com.dilema.modelo.Caso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CasoRepo extends JpaRepository<Caso, Long> {
    List<Caso> findByDocenteIdOrderByIdDesc(Long docenteId);
}
