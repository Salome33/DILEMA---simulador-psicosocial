package com.dilema.servicio;

import com.dilema.dto.Contratos;
import com.dilema.modelo.Catalogo;
import com.dilema.modelo.SesionCaso;
import com.dilema.modelo.Usuario;
import com.dilema.repo.ParticipacionRepo;
import com.dilema.repo.SesionEstudianteRepo;
import com.dilema.repo.SesionRepo;
import com.dilema.repo.UsuarioRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthServicio {
    private final UsuarioRepo usuarios;
    private final SesionRepo sesiones;
    private final ParticipacionRepo participaciones;
    private final SesionEstudianteRepo autorizados;
    private final Tokens tokens;
    private final Mapeador mapeador;
    private final PasswordEncoder cifrado;
    private final GestorAcceso gestor = new GestorAcceso();

    public AuthServicio(UsuarioRepo usuarios, SesionRepo sesiones, ParticipacionRepo participaciones,
                        SesionEstudianteRepo autorizados, Tokens tokens, Mapeador mapeador, PasswordEncoder cifrado) {
        this.usuarios = usuarios;
        this.sesiones = sesiones;
        this.participaciones = participaciones;
        this.autorizados = autorizados;
        this.tokens = tokens;
        this.mapeador = mapeador;
        this.cifrado = cifrado;
    }

    @Transactional
    public Contratos.SesionAuth ingresar(Contratos.Ingreso pedido) {
        Resultado resultado = gestor.elegir(pedido).ingresar(pedido);
        String token = tokens.emitir(resultado.usuario().id);
        Long sesionId = resultado.sesion() == null ? null : resultado.sesion().id;
        return new Contratos.SesionAuth(token, mapeador.usuario(resultado.usuario()), sesionId);
    }

    public record Resultado(Usuario usuario, SesionCaso sesion) {}

    /** Strategy (RF-01): el estudiante entra con código; docente y administración, con clave. */
    class GestorAcceso {
        Autenticacion elegir(Contratos.Ingreso pedido) {
            if (pedido.codigoAutorizacion() != null && !pedido.codigoAutorizacion().isBlank()) {
                return new AutenticacionEstudiante();
            }
            return new AutenticacionCuenta();
        }
    }

    interface Autenticacion {
        Resultado ingresar(Contratos.Ingreso pedido);
    }

    class AutenticacionEstudiante implements Autenticacion {
        @Override
        public Resultado ingresar(Contratos.Ingreso pedido) {
            Usuario usuario = usuarios.findByEmailIgnoreCase(nulo(pedido.email()))
                    .filter(u -> u.activo && u.rol == Catalogo.Rol.ESTUDIANTE)
                    .orElseThrow(() -> new ReglaNegocio("No hay un estudiante activo con ese correo institucional."));
            SesionCaso sesion = sesiones.findByCodigoIgnoreCase(nulo(pedido.codigoAutorizacion()))
                    .orElseThrow(() -> new ReglaNegocio("El código de autorización no corresponde a una convocatoria."));
            if (sesion.caso.estado != Catalogo.EstadoCaso.PUBLICADO) {
                throw new ReglaNegocio("El caso de esta convocatoria no está publicado.");
            }
            if (!autorizados.existsBySesionIdAndEstudianteId(sesion.id, usuario.id)) {
                throw new ReglaNegocio("Este código pertenece a una convocatoria en la que no estás autorizado.");
            }
            if (sesion.estado() == Catalogo.EstadoSesion.SIN_INICIAR) {
                throw new ReglaNegocio("La sesión todavía no ha sido iniciada por el docente.");
            }
            boolean yaParticipa = !participaciones.findByEstudianteIdAndSesionId(usuario.id, sesion.id).isEmpty();
            if (sesion.estado() == Catalogo.EstadoSesion.EN_CURSO && !yaParticipa && !sesion.permiteIngreso(LocalDateTime.now())) {
                throw new ReglaNegocio("RF-22: ya pasó el tiempo para iniciar la participación.");
            }
            return new Resultado(usuario, sesion);
        }
    }

    class AutenticacionCuenta implements Autenticacion {
        @Override
        public Resultado ingresar(Contratos.Ingreso pedido) {
            if ("ESTUDIANTE".equalsIgnoreCase(rolNormalizado(pedido.rol()))) {
                throw new ReglaNegocio("El estudiante ingresa con el código de autorización de la convocatoria.");
            }
            Usuario usuario = usuarios.findByEmailIgnoreCase(nulo(pedido.email()))
                    .filter(u -> u.activo)
                    .orElseThrow(() -> new ReglaNegocio("Correo o clave incorrectos."));
            if (usuario.rol == Catalogo.Rol.ESTUDIANTE || usuario.contrasena == null) {
                throw new ReglaNegocio("El estudiante ingresa con el código de autorización de la convocatoria.");
            }
            if (pedido.clave() == null || !cifrado.matches(pedido.clave(), usuario.contrasena)) {
                throw new ReglaNegocio("Correo o clave incorrectos.");
            }
            String pedidoRol = rolNormalizado(pedido.rol());
            if (!pedidoRol.isBlank() && !usuario.rol.name().equals(pedidoRol)) {
                throw new ReglaNegocio("La cuenta no corresponde al rol seleccionado.");
            }
            return new Resultado(usuario, null);
        }
    }

    private static String rolNormalizado(String rol) {
        if (rol == null || rol.isBlank()) return "";
        return switch (rol.trim().toUpperCase()) {
            case "DOCENTE", "PROFESOR" -> "PROFESOR";
            case "ADMIN", "ADMINISTRADOR" -> "ADMIN";
            default -> rol.trim().toUpperCase();
        };
    }

    private static String nulo(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
