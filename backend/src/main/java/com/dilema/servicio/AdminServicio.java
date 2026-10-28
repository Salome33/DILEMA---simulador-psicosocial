package com.dilema.servicio;

import com.dilema.dto.Contratos;
import com.dilema.modelo.Catalogo;
import com.dilema.modelo.Usuario;
import com.dilema.repo.GrupoRepo;
import com.dilema.repo.UsuarioRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminServicio {
    private final UsuarioRepo usuarios;
    private final GrupoRepo grupos;
    private final ProxyOperacion proxy;
    private final Mapeador mapeador;
    private final FabricaCuentas fabrica;
    private final PasswordEncoder cifrado;

    public AdminServicio(UsuarioRepo usuarios, GrupoRepo grupos, ProxyOperacion proxy,
                         Mapeador mapeador, FabricaCuentas fabrica, PasswordEncoder cifrado) {
        this.usuarios = usuarios;
        this.grupos = grupos;
        this.proxy = proxy;
        this.mapeador = mapeador;
        this.fabrica = fabrica;
        this.cifrado = cifrado;
    }

    @Transactional(readOnly = true)
    public List<Contratos.UsuarioVista> usuarios(Usuario admin) {
        proxy.gestionarUsuarios(admin);
        List<Usuario> cuentas = new ArrayList<>();
        cuentas.addAll(usuarios.findByRol(Catalogo.Rol.ADMIN));
        cuentas.addAll(usuarios.findByRol(Catalogo.Rol.PROFESOR));
        return cuentas.stream().map(mapeador::usuario).toList();
    }

    /** El administrador crea docentes. El estudiante lo registra el docente de su grupo. */
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
        if (rol == Catalogo.Rol.ESTUDIANTE) {
            throw new ReglaNegocio("El docente registra y matricula a los estudiantes.");
        }
        if (entrada.clave() == null || entrada.clave().length() < 6) {
            throw new ReglaNegocio("La clave debe tener al menos 6 caracteres.");
        }
        Usuario nuevo = rol == Catalogo.Rol.PROFESOR ? fabrica.nuevoDocente() : fabrica.nuevoAdministrador();
        nuevo.nombre = entrada.nombre().trim();
        nuevo.email = entrada.email().trim().toLowerCase();
        nuevo.contrasena = cifrado.encode(entrada.clave());
        usuarios.save(nuevo);
        return mapeador.usuario(nuevo);
    }

    @Transactional
    public Contratos.UsuarioVista permiso(Usuario admin, Long id, List<String> extras) {
        proxy.gestionarUsuarios(admin);
        Usuario docente = usuarios.findById(id).orElseThrow(() -> new NoEncontrado("Docente no encontrado."));
        if (docente.rol != Catalogo.Rol.PROFESOR) {
            throw new ReglaNegocio("Solo se administran los permisos de un docente.");
        }
        fabrica.reemplazarPermisosDocente(docente, extras);
        return mapeador.usuario(docente);
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
            default -> throw new ReglaNegocio("El rol debe ser docente o administración.");
        };
    }
}
