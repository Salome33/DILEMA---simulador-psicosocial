package com.dilema.servicio;

import com.dilema.modelo.Catalogo;
import com.dilema.modelo.Permiso;
import com.dilema.modelo.Usuario;
import com.dilema.repo.PermisoRepo;
import com.dilema.repo.UsuarioRepo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory Method (RF-03). {@code crear} fija el alta; cada creador decide el rol
 * y los permisos de nacimiento. Las pantallas no arman el usuario ni esos permisos.
 */
@Component
public class FabricaCuentas {
    public static final String VISUALIZAR = "VISUALIZAR";
    public static final String EDITAR = "EDITAR";
    public static final String GESTIONAR_ESPACIOS = "GESTIONAR_ESPACIOS_ACADEMICOS";

    private final PermisoRepo permisos;
    private final UsuarioRepo usuarios;

    public FabricaCuentas(PermisoRepo permisos, UsuarioRepo usuarios) {
        this.permisos = permisos;
        this.usuarios = usuarios;
    }

    public void asegurarCatalogo() {
        for (String tipo : List.of(VISUALIZAR, EDITAR, GESTIONAR_ESPACIOS)) {
            requerir(tipo);
        }
    }

    public Usuario nuevoDocente() {
        return new CreadorDocente().crear(this);
    }

    public Usuario nuevoAdministrador() {
        return new CreadorAdministrador().crear(this);
    }

    public Usuario nuevoEstudiante(String nombre, String email) {
        Usuario usuario = new CreadorEstudiante().crear(this);
        usuario.nombre = nombre.trim();
        usuario.email = email.trim().toLowerCase();
        return usuarios.save(usuario);
    }

    /** El docente conserva Visualizar. Editar y gestionar espacios se suman o se quitan. */
    public void reemplazarPermisosDocente(Usuario docente, List<String> extras) {
        docente.permisos.clear();
        docente.permisos.add(requerir(VISUALIZAR));
        if (extras != null) {
            for (String extra : extras) {
                if (EDITAR.equals(extra) || GESTIONAR_ESPACIOS.equals(extra)) {
                    docente.permisos.add(requerir(extra));
                }
            }
        }
        usuarios.save(docente);
    }

    public void otorgarSiVacio(Usuario usuario) {
        if (!usuario.permisos.isEmpty()) return;
        if (usuario.rol == Catalogo.Rol.PROFESOR) usuario.permisos.add(requerir(VISUALIZAR));
        if (usuario.rol == Catalogo.Rol.ADMIN) usuario.permisos.add(requerir(GESTIONAR_ESPACIOS));
        usuarios.save(usuario);
    }

    Permiso requerir(String tipo) {
        return permisos.findByTipo(tipo).orElseGet(() -> {
            Permiso permiso = new Permiso();
            permiso.tipo = tipo;
            return permisos.save(permiso);
        });
    }

    abstract static class CreadorUsuario {
        final Usuario crear(FabricaCuentas fabrica) {
            Usuario usuario = fabricar();
            usuario.permisos.addAll(permisosDeNacimiento(fabrica));
            return usuario;
        }

        abstract Usuario fabricar();

        abstract List<Permiso> permisosDeNacimiento(FabricaCuentas fabrica);
    }

    /** El docente nace solo con Visualizar. Editar no sale de esta fábrica. */
    static class CreadorDocente extends CreadorUsuario {
        @Override
        Usuario fabricar() {
            Usuario usuario = new Usuario();
            usuario.rol = Catalogo.Rol.PROFESOR;
            usuario.activo = true;
            return usuario;
        }

        @Override
        List<Permiso> permisosDeNacimiento(FabricaCuentas fabrica) {
            return List.of(fabrica.requerir(VISUALIZAR));
        }
    }

    static class CreadorAdministrador extends CreadorUsuario {
        @Override
        Usuario fabricar() {
            Usuario usuario = new Usuario();
            usuario.rol = Catalogo.Rol.ADMIN;
            usuario.activo = true;
            return usuario;
        }

        @Override
        List<Permiso> permisosDeNacimiento(FabricaCuentas fabrica) {
            return List.of(fabrica.requerir(GESTIONAR_ESPACIOS));
        }
    }

    /** El estudiante nace sin contraseña y sin permisos de catálogo. */
    static class CreadorEstudiante extends CreadorUsuario {
        @Override
        Usuario fabricar() {
            Usuario usuario = new Usuario();
            usuario.rol = Catalogo.Rol.ESTUDIANTE;
            usuario.activo = true;
            return usuario;
        }

        @Override
        List<Permiso> permisosDeNacimiento(FabricaCuentas fabrica) {
            return List.of();
        }
    }
}
