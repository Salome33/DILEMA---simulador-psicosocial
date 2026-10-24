package com.dilema.servicio;

import com.dilema.modelo.Catalogo;
import com.dilema.modelo.Usuario;
import org.springframework.stereotype.Component;

/** Proxy de protección (RF-02): el rol autoriza la operación antes de ejecutarla. */
@Component
public class ProxyOperacion {
    public void crearCaso(Usuario usuario) {
        if (usuario.rol != Catalogo.Rol.PROFESOR) {
            throw new AccesoDenegado("RF-02: crear casos es una función del docente.");
        }
    }

    public void revisarGrupo(Usuario usuario) {
        if (usuario.rol != Catalogo.Rol.PROFESOR) {
            throw new AccesoDenegado("RF-02: revisar el grupo es una función del docente.");
        }
    }

    public void gestionarUsuarios(Usuario usuario) {
        if (usuario.rol != Catalogo.Rol.ADMIN) {
            throw new AccesoDenegado("RF-02: administrar usuarios es una función de administración.");
        }
    }

    public void participar(Usuario usuario) {
        if (usuario.rol != Catalogo.Rol.ESTUDIANTE) {
            throw new AccesoDenegado("RF-02: participar es una función del estudiante.");
        }
    }
}
