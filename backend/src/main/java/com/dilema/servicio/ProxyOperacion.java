package com.dilema.servicio;

import com.dilema.modelo.Catalogo;
import com.dilema.modelo.Usuario;
import org.springframework.stereotype.Component;

/**
 * Proxy de protección (RF-02). La autorización vive en esta puerta.
 * El alta del caso, la matrícula y el grupo se escriben en el sujeto, no dentro del if.
 */
@Component
public class ProxyOperacion {
    public void crearCaso(Usuario usuario) {
        exigirRol(usuario, Catalogo.Rol.PROFESOR);
        exigir(usuario, FabricaCuentas.EDITAR);
    }

    public void revisarGrupo(Usuario usuario) {
        exigirRol(usuario, Catalogo.Rol.PROFESOR);
        exigir(usuario, FabricaCuentas.VISUALIZAR);
    }

    public void gestionarEspacio(Usuario usuario) {
        exigirRol(usuario, Catalogo.Rol.PROFESOR);
        exigir(usuario, FabricaCuentas.GESTIONAR_ESPACIOS);
    }

    public void gestionarUsuarios(Usuario usuario) {
        exigirRol(usuario, Catalogo.Rol.ADMIN);
    }

    public void participar(Usuario usuario) {
        exigirRol(usuario, Catalogo.Rol.ESTUDIANTE);
    }

    private void exigirRol(Usuario usuario, Catalogo.Rol rol) {
        if (usuario == null || usuario.rol != rol) {
            throw new AccesoDenegado("No tienes permiso para esta operación.");
        }
    }

    private void exigir(Usuario usuario, String permiso) {
        if (!usuario.tiene(permiso)) {
            throw new AccesoDenegado("No tienes el permiso requerido.");
        }
    }
}
