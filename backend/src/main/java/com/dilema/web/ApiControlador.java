package com.dilema.web;

import com.dilema.dto.Contratos;
import com.dilema.modelo.Usuario;
import com.dilema.servicio.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiControlador {
    private final AuthServicio auth;
    private final CasoServicio casos;
    private final AcademiaServicio academia;
    private final ParticipacionServicio participaciones;
    private final AdminServicio admin;
    private final Tokens tokens;
    private final Mapeador mapeador;

    public ApiControlador(AuthServicio auth, CasoServicio casos, AcademiaServicio academia,
                          ParticipacionServicio participaciones, AdminServicio admin,
                          Tokens tokens, Mapeador mapeador) {
        this.auth = auth;
        this.casos = casos;
        this.academia = academia;
        this.participaciones = participaciones;
        this.admin = admin;
        this.tokens = tokens;
        this.mapeador = mapeador;
    }

    @GetMapping("/salud")
    Map<String, String> salud() { return Map.of("estado", "ok"); }

    @PostMapping("/auth/ingresar")
    Contratos.SesionAuth ingresar(@RequestBody Contratos.Ingreso ingreso) { return auth.ingresar(ingreso); }

    @GetMapping("/auth/yo")
    Contratos.UsuarioVista yo(HttpServletRequest request) { return mapeador.usuario(FiltroToken.actual(request)); }

    @DeleteMapping("/auth/sesion")
    void salir(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) tokens.revocar(header.substring(7));
    }

    @GetMapping("/estudiante/panel")
    Contratos.PanelEstudiante panel(HttpServletRequest request) {
        return participaciones.panel(FiltroToken.actual(request));
    }

    @PostMapping("/estudiante/avisos/{id}/leer")
    void leerAviso(@PathVariable Long id, HttpServletRequest request) {
        participaciones.leerAviso(FiltroToken.actual(request), id);
    }

    @PostMapping("/estudiante/sesiones/{id}/participaciones")
    Contratos.ParticipacionVista iniciar(@PathVariable Long id, HttpServletRequest request) {
        return participaciones.iniciar(FiltroToken.actual(request), id);
    }

    @GetMapping("/participaciones/{id}")
    Contratos.ParticipacionVista ver(@PathVariable Long id, HttpServletRequest request) {
        return participaciones.ver(FiltroToken.actual(request), id);
    }

    @PostMapping("/participaciones/{id}/responder")
    Contratos.ParticipacionVista responder(@PathVariable Long id, @RequestBody Contratos.RespuestaIn cuerpo, HttpServletRequest request) {
        return participaciones.responder(FiltroToken.actual(request), id, cuerpo.preguntaId(), cuerpo.opcionId());
    }

    @PostMapping("/participaciones/{id}/deshacer")
    Contratos.ParticipacionVista deshacer(@PathVariable Long id, HttpServletRequest request) {
        return participaciones.deshacer(FiltroToken.actual(request), id);
    }

    @PostMapping("/participaciones/{id}/finalizar")
    Contratos.ParticipacionVista finalizar(@PathVariable Long id, HttpServletRequest request) {
        return participaciones.finalizar(FiltroToken.actual(request), id);
    }

    @GetMapping("/docente/casos")
    List<Contratos.CasoResumen> casos(HttpServletRequest request) { return casos.listar(docente(request)); }

    @GetMapping("/docente/casos/{id}")
    Contratos.CasoVista caso(@PathVariable Long id, HttpServletRequest request) { return casos.obtener(docente(request), id); }

    @PostMapping("/docente/casos")
    Contratos.CasoVista crearCaso(@RequestBody Contratos.CasoIn cuerpo, HttpServletRequest request) {
        return casos.crear(docente(request), cuerpo);
    }

    @PutMapping("/docente/casos/{id}")
    Contratos.CasoVista editarCaso(@PathVariable Long id, @RequestBody Contratos.CasoIn cuerpo, HttpServletRequest request) {
        return casos.actualizar(docente(request), id, cuerpo);
    }

    @PostMapping("/docente/casos/{id}/publicar")
    Contratos.CasoVista publicar(@PathVariable Long id, HttpServletRequest request) { return casos.publicar(docente(request), id); }

    @PostMapping("/docente/casos/{id}/archivar")
    Contratos.CasoVista archivar(@PathVariable Long id, HttpServletRequest request) { return casos.archivar(docente(request), id); }

    @PostMapping("/docente/casos/{id}/despublicar")
    Contratos.CasoVista despublicar(@PathVariable Long id, HttpServletRequest request) { return casos.despublicar(docente(request), id); }

    @PostMapping("/docente/casos/{id}/duplicar")
    Contratos.CasoVista duplicar(@PathVariable Long id, HttpServletRequest request) { return casos.duplicar(docente(request), id); }

    @GetMapping("/docente/grupos")
    List<Contratos.GrupoVista> grupos(HttpServletRequest request) { return academia.listarGrupos(docente(request)); }

    @PostMapping("/docente/grupos")
    Contratos.GrupoVista crearGrupo(@RequestBody Contratos.GrupoIn cuerpo, HttpServletRequest request) {
        return academia.crearGrupo(docente(request), cuerpo);
    }

    @PostMapping("/docente/grupos/{id}/estudiantes")
    Contratos.GrupoVista agregar(@PathVariable Long id, @RequestBody Contratos.EstudianteIn cuerpo, HttpServletRequest request) {
        return academia.agregarEstudiante(docente(request), id, cuerpo.email());
    }

    @DeleteMapping("/docente/grupos/{id}/estudiantes/{estudianteId}")
    Contratos.GrupoVista retirar(@PathVariable Long id, @PathVariable Long estudianteId, HttpServletRequest request) {
        return academia.retirarEstudiante(docente(request), id, estudianteId);
    }

    @GetMapping("/docente/sesiones")
    List<Contratos.SesionVista> sesiones(HttpServletRequest request) { return academia.sesionesDocente(docente(request)); }

    @PostMapping("/docente/sesiones")
    Contratos.SesionVista agendar(@RequestBody Contratos.AgendaIn cuerpo, HttpServletRequest request) {
        return academia.agendar(docente(request), cuerpo);
    }

    @PostMapping("/docente/sesiones/{id}/iniciar")
    Contratos.SesionVista iniciarSesion(@PathVariable Long id, HttpServletRequest request) {
        return academia.iniciar(docente(request), id);
    }

    @PostMapping("/docente/sesiones/{id}/finalizar")
    Contratos.SesionVista finalizarSesion(@PathVariable Long id, HttpServletRequest request) {
        return academia.finalizar(docente(request), id);
    }

    @GetMapping("/docente/sesiones/{id}/seguimiento")
    List<Contratos.FilaSeguimiento> seguimiento(@PathVariable Long id, HttpServletRequest request) {
        return participaciones.seguimiento(docente(request), id);
    }

    @GetMapping(value = "/docente/sesiones/{id}/reporte.csv", produces = "text/csv")
    ResponseEntity<String> reporte(@PathVariable Long id, HttpServletRequest request) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-sesion-" + id + ".csv")
                .body(participaciones.reporte(docente(request), id));
    }

    @PostMapping("/docente/participaciones/{id}/retroalimentacion")
    void retro(@PathVariable Long id, @RequestBody Contratos.TextoIn cuerpo, HttpServletRequest request) {
        participaciones.retroalimentar(docente(request), id, cuerpo.texto());
    }

    @PostMapping("/docente/participaciones/{id}/repeticion")
    void repeticion(@PathVariable Long id, HttpServletRequest request) {
        participaciones.autorizarRepeticion(docente(request), id);
    }

    @GetMapping("/admin/usuarios")
    List<Contratos.UsuarioVista> usuarios(HttpServletRequest request) { return admin.usuarios(admin(request)); }

    @PostMapping("/admin/usuarios")
    Contratos.UsuarioVista crearUsuario(@RequestBody Contratos.UsuarioIn cuerpo, HttpServletRequest request) {
        return admin.crear(admin(request), cuerpo);
    }

    @PostMapping("/admin/usuarios/{id}/permisos")
    Contratos.UsuarioVista permiso(@PathVariable Long id, @RequestBody Contratos.PermisoIn cuerpo, HttpServletRequest request) {
        return admin.permiso(admin(request), id, cuerpo);
    }

    @GetMapping("/admin/grupos")
    List<Contratos.GrupoVista> gruposAdmin(HttpServletRequest request) { return admin.grupos(admin(request)); }

    @GetMapping("/admin/auditoria")
    List<Contratos.AuditoriaVista> auditoria(HttpServletRequest request) { return admin.auditoria(admin(request)); }

    private Usuario docente(HttpServletRequest request) { return FiltroToken.actual(request); }
    private Usuario admin(HttpServletRequest request) { return FiltroToken.actual(request); }
}
