package com.dilema.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class Contratos {
    private Contratos() {}

    public record Ingreso(String email, String clave, String codigoAutorizacion, String rol) {}
    public record UsuarioVista(Long id, String nombre, String email, String rol, List<String> permisos) {}
    public record SesionAuth(String token, UsuarioVista usuario, Long sesionId) {}

    public record OpcionIn(Long id, String texto, boolean correcta, String explicacion, String fundamento) {}
    public record PreguntaIn(Long id, String enunciado, String pista, String criterio, Double peso, List<OpcionIn> opciones) {}
    public record BloqueIn(Long id, String nombre, String contexto, List<PreguntaIn> preguntas) {}
    public record PersonajeIn(Long id, String nombre, String descripcion, List<Integer> escenarios) {}
    public record CriterioIn(Long id, String nombre, double peso, String descripcion) {}
    public record CasoIn(String nombre, String desarrolloSituacional, String contexto, Integer tiempoEstimadoMin,
                         boolean sensible, String advertencia, List<BloqueIn> bloques, List<CriterioIn> rubrica,
                         List<PersonajeIn> personajes) {}
    public record CasoVista(Long id, String nombre, String desarrolloSituacional, String contexto, Integer tiempoEstimadoMin,
                            boolean sensible, String advertencia, String estado, Long docenteId,
                            List<BloqueIn> bloques, List<CriterioIn> rubrica, List<PersonajeIn> personajes) {}
    public record CasoResumen(Long id, String nombre, String estado, Integer tiempoEstimadoMin, int bloques, int preguntas) {}

    public record GrupoIn(String nombre, String espacioAcademico) {}
    public record EstudianteIn(String email) {}
    public record PersonaVista(Long id, String nombre, String email) {}
    public record GrupoVista(Long id, String nombre, String espacioAcademico, PersonaVista docente, List<PersonaVista> estudiantes) {}

    public record AgendaIn(Long casoId, Long grupoId, String inicio, String fin, int tiempoMaxMin, int toleranciaMin,
                           String lugar, String mensaje) {}
    public record SesionVista(Long id, String codigo, String estado, String inicio, String fin, int tiempoMaxMin,
                              int toleranciaMin, String lugar, String mensaje, Long casoId, String caso,
                              Long grupoId, String grupo, String espacio, String desarrollo, String contexto,
                              boolean sensible, String advertencia, Long participacionId, String participacionEstado,
                              Double nota, Integer progreso) {}

    public record UsuarioIn(String nombre, String email, String clave, String rol) {}
    public record PermisoIn(String permiso, boolean activo) {}

    public record Aviso(Long id, String canal, String titulo, String cuerpo, boolean leida, String fecha) {}
    public record AuditoriaVista(Long id, String fecha, String accion, String detalle, String usuario) {}

    public record OpcionJuego(Long id, String texto, Boolean correcta, String explicacion, String fundamento) {}
    public record PreguntaJuego(Long id, String bloque, String contexto, String enunciado, String pista, String criterio,
                                Long seleccionId, List<OpcionJuego> opciones) {}
    public record ParticipacionVista(Long id, Long sesionId, String estado, String caso, int progreso, int total,
                                     int segundosMax, long segundosUsados, Double nota, String retroalimentacion,
                                     String estudiante, List<PreguntaJuego> preguntas) {}
    public record RespuestaIn(Long preguntaId, Long opcionId) {}
    public record TextoIn(String texto) {}
    public record PanelEstudiante(List<SesionVista> sesiones, List<Aviso> avisos, int sinLeer) {}
    public record FilaSeguimiento(Long participacionId, Long estudianteId, String estudiante, String email,
                                  String estado, Double nota, String retroalimentacion) {}
}
