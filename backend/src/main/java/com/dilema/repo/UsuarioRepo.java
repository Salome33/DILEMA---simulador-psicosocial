package com.dilema.repo;

import com.dilema.modelo.Catalogo;
import com.dilema.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepo extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    List<Usuario> findByRol(Catalogo.Rol rol);
}
