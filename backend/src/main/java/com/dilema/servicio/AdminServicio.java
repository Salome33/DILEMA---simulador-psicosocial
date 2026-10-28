package com.dilema.servicio;

import com.dilema.dto.Contratos;
import com.dilema.modelo.Catalogo;
import com.dilema.modelo.Usuario;
import com.dilema.repo.GrupoRepo;
import com.dilema.repo.UsuarioRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminServicio {
    private final UsuarioRepo usuarios;
    private final GrupoRepo grupos;
    private final ProxyOperacion proxy;
    private final AuthServicio auth;
    private final Mapeador mapeador;

    public AdminServicio(UsuarioRepo usuarios, GrupoRepo grupos, ProxyOperacion proxy,
                         AuthServicio auth, Mapeador mapeador) {
        this.usuarios = usuarios;
        this.grupos = grupos;
        this.proxy = proxy;
        this.auth = auth;
        this.mapeador = mapeador;
    }

    @Transactional(readOnly = true)
    public List<Contratos.UsuarioVista> usuarios(Usuario admin) {
        proxy.gestionarUsuarios(admin);
        return usuarios.findAll().stream().map(mapeador::usuario).toList();
    }

    /** Factory Method (RF-03): el alta elige el rol y deja la clave vacía si es estudiante. */
    @Transactional
    public Contratos.UsuarioVista crear(Usuario admin, Contratos.UsuarioIn entrada) {
        proxy.gestionarUsuarios(admin);
        if (entrada.nombre() == null || entrada.email() == null) {
            throw new ReglaNegocio("Indica nombre y correo institucional.");
        }
        if (usuarios.findByEmailIgnoreCase(entrada.email().trim()).isPresent()) {
            throw new ReglaNegocio("Ya existe una cuenta con ese correo.");
        }
        Catalogo.Rol rol = rolDe(entrada.rol());
        if (rol != Catalogo.Rol.ESTUDIANTE && (entrada.clave() == null || entrada.clave().length() < 6)) {
            throw new ReglaNegocio("Docente y administración necesitan una clave de al menos 6 caracteres.");
        }
        Usuario nuevo = fabricar(rol);
        nuevo.nombre = entrada.nombre().trim();
        nuevo.email = entrada.email().trim().toLowerCase();
        if (rol != Catalogo.Rol.ESTUDIANTE) nuevo.contrasena = auth.cifrado().encode(entrada.clave());
        usuarios.save(nuevo);
        return mapeador.usuario(nuevo);
    }

    @Transactional
    public Contratos.UsuarioVista permiso(Usuario admin, Long id, Contratos.PermisoIn entrada) {
        proxy.gestionarUsuarios(admin);
        throw new ReglaNegocio("RF-02: lo que puede hacer cada persona lo define su rol, no un permiso aparte.");
    }

    @Transactional(readOnly = true)
    public List<Contratos.GrupoVista> grupos(Usuario admin) {
        proxy.gestionarUsuarios(admin);
        return grupos.findAll().stream().map(mapeador::grupo).toList();
    }

    @Transactional(readOnly = true)
    public List<Contratos.AuditoriaVista> auditoria(Usuario admin) {
        proxy.gestionarUsuarios(admin);
        return List.of();
    }

    private Catalogo.Rol rolDe(String texto) {
        if (texto == null) throw new ReglaNegocio("Indica el rol.");
        return switch (texto.trim().toUpperCase()) {
            case "DOCENTE", "PROFESOR" -> Catalogo.Rol.PROFESOR;
            case "ADMIN", "ADMINISTRADOR" -> Catalogo.Rol.ADMIN;
            case "ESTUDIANTE" -> Catalogo.Rol.ESTUDIANTE;
            default -> throw new ReglaNegocio("El rol debe ser estudiante, docente o administración.");
        };
    }

    private Usuario fabricar(Catalogo.Rol rol) {
        CreadorUsuario creador = switch (rol) {
            case PROFESOR -> new CreadorDocente();
            case ADMIN -> new CreadorAdministrador();
            case ESTUDIANTE -> new CreadorEstudiante();
        };
        return creador.crear();
    }

    interface CreadorUsuario { Usuario crear(); }

    static class CreadorDocente implements CreadorUsuario {
        public Usuario crear() {
            Usuario usuario = new Usuario();
            usuario.rol = Catalogo.Rol.PROFESOR;
            usuario.activo = true;
            return usuario;
        }
    }

    static class CreadorAdministrador implements CreadorUsuario {
        public Usuario crear() {
            Usuario usuario = new Usuario();
            usuario.rol = Catalogo.Rol.ADMIN;
            usuario.activo = true;
            return usuario;
        }
    }

    static class CreadorEstudiante implements CreadorUsuario {
        public Usuario crear() {
            Usuario usuario = new Usuario();
            usuario.rol = Catalogo.Rol.ESTUDIANTE;
            usuario.activo = true;
            return usuario;
        }
    }
}
