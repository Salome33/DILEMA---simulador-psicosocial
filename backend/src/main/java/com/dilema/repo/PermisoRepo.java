package com.dilema.repo;

import com.dilema.modelo.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermisoRepo extends JpaRepository<Permiso, Long> {
    Optional<Permiso> findByTipo(String tipo);
}
