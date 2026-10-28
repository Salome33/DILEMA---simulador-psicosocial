package com.dilema.servicio;

import com.dilema.dto.Contratos;
import com.dilema.modelo.*;
import com.dilema.repo.CasoRepo;
import com.dilema.repo.GrupoRepo;
import com.dilema.repo.ParticipacionRepo;
import com.dilema.repo.SesionEstudianteRepo;
import com.dilema.repo.SesionRepo;
import com.dilema.repo.UsuarioRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AcademiaServicio {
    private final GrupoRepo grupos;
    private final CasoRepo casos;
    private final SesionRepo sesiones;
    private final UsuarioRepo usuarios;
    private final ParticipacionRepo participaciones;
    private final SesionEstudianteRepo autorizados;
    private final ParticipacionServicio participacionServicio;
    private final ProxyOperacion proxy;
    private final BitacoraServicio bitacora;
    private final Mapeador mapeador;

    public AcademiaServicio(GrupoRepo grupos, CasoRepo casos, SesionRepo sesiones, UsuarioRepo usuarios,
                            ParticipacionRepo participaciones, SesionEstudianteRepo autorizados,
                            ParticipacionServicio participacionServicio,
                            ProxyOperacion proxy, BitacoraServicio bitacora, Mapeador mapeador) {
        this.grupos = grupos;
        this.casos = casos;
        this.sesiones = sesiones;
        this.usuarios = usuarios;
        this.participaciones = participaciones;
        this.autorizados = autorizados;
        this.participacionServicio = participacionServicio;
        this.proxy = proxy;
        this.bitacora = bitacora;
        this.mapeador = mapeador;
    }

    @Transactional(readOnly = true)
    public List<Contratos.GrupoVista> listarGrupos(Usuario docente) {
        proxy.revisarGrupo(docente);
        return grupos.findByDocenteId(docente.id).stream().map(mapeador::grupo).toList();
    }

    @Transactional
    public Contratos.GrupoVista crearGrupo(Usuario docente, Contratos.GrupoIn entrada) {
        proxy.revisarGrupo(docente);
        if (entrada.nombre() == null || entrada.nombre().isBlank() || entrada.espacioAcademico() == null || entrada.espacioAcademico().isBlank()) {
            throw new ReglaNegocio("El grupo necesita nombre y semestre.");
        }
        Grupo grupo = new Grupo();
        grupo.nombre = entrada.nombre().trim();
        grupo.semestre = entrada.espacioAcademico().trim();
        grupo.docente = docente;
        grupos.save(grupo);
        bitacora.auditar(docente, "CREAR_GRUPO", grupo.nombre);
        return mapeador.grupo(grupo);
    }

    @Transactional
    public Contratos.GrupoVista agregarEstudiante(Usuario docente, Long grupoId, String email) {
        proxy.revisarGrupo(docente);
        Grupo grupo = propio(docente, grupoId);
        Usuario estudiante = usuarios.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .filter(u -> u.rol == Catalogo.Rol.ESTUDIANTE && u.activo)
                .orElseThrow(() -> new ReglaNegocio("No existe un estudiante activo con ese correo."));
        if (grupo.pertenece(estudiante)) throw new ReglaNegocio("El estudiante ya está en el grupo.");
        grupo.estudiantes.add(estudiante);
        String codigo = sesiones.findByGrupo_Docente_IdOrderByIdDesc(docente.id).stream()
                .filter(s -> s.grupo.id.equals(grupo.id) && s.estado() != Catalogo.EstadoSesion.FINALIZADO)
                .map(s -> s.codigo).findFirst().orElse("se informará cuando se programe la sesión");
        sesiones.findByGrupo_Docente_IdOrderByIdDesc(docente.id).stream()
                .filter(s -> s.grupo.id.equals(grupo.id) && s.cierreReal == null)
                .forEach(s -> autorizar(s, estudiante));
        bitacora.avisar(estudiante, "CORREO", "Convocatoria actualizada",
                "Fuiste agregado al espacio " + grupo.nombre + ". Código de acceso: " + codigo + ". Entra en /ingreso.");
        bitacora.auditar(docente, "AGREGAR_ESTUDIANTE", estudiante.email);
        return mapeador.grupo(grupo);
    }

    @Transactional
    public Contratos.GrupoVista retirarEstudiante(Usuario docente, Long grupoId, Long estudianteId) {
        proxy.revisarGrupo(docente);
        Grupo grupo = propio(docente, grupoId);
        Usuario estudiante = grupo.estudiantes.stream().filter(e -> e.id.equals(estudianteId)).findFirst()
                .orElseThrow(() -> new NoEncontrado("El estudiante no está en el grupo."));
        grupo.estudiantes.remove(estudiante);
        sesiones.findByGrupo_Docente_IdOrderByIdDesc(docente.id).stream()
                .filter(s -> s.grupo.id.equals(grupo.id) && s.cierreReal == null)
                .forEach(s -> autorizados.deleteBySesionIdAndEstudianteId(s.id, estudiante.id));
        bitacora.avisar(estudiante, "CORREO", "Cambio de convocatoria",
                "Fuiste retirado del espacio " + grupo.nombre + ".");
        bitacora.auditar(docente, "RETIRAR_ESTUDIANTE", estudiante.email);
        return mapeador.grupo(grupo);
    }

    /** Facade (RF-04): una sola operación agenda el caso, el grupo y los estudiantes. */
    @Transactional
    public Contratos.SesionVista agendar(Usuario docente, Contratos.AgendaIn entrada) {
        proxy.revisarGrupo(docente);
        return new FachadaAgendamiento().agendar(docente, entrada);
    }

    class FachadaAgendamiento {
        Contratos.SesionVista agendar(Usuario docente, Contratos.AgendaIn entrada) {
            Caso caso = casos.findById(entrada.casoId()).orElseThrow(() -> new NoEncontrado("Caso no encontrado."));
            Grupo grupo = grupos.findById(entrada.grupoId()).orElseThrow(() -> new NoEncontrado("Grupo no encontrado."));
            LocalDateTime inicio = parse(entrada.inicio());
            LocalDateTime fin = parse(entrada.fin());
            ContextoAgenda contexto = new ContextoAgenda(docente, caso, grupo, inicio, fin, entrada.tiempoMaxMin(), entrada.toleranciaMin());
            new CadenaAgendamiento().validar(contexto);
            SesionCaso sesion = new SesionCaso();
            sesion.caso = caso;
            sesion.grupo = grupo;
            sesion.inicio = inicio;
            sesion.fin = fin;
            sesion.tiempoMaxMin = entrada.tiempoMaxMin();
            sesion.toleranciaMin = entrada.toleranciaMin();
            sesion.lugar = entrada.lugar();
            sesion.mensaje = entrada.mensaje();
            sesion.codigo = new FabricaCodigoAlfanumerico().emitir(sesiones);
            sesiones.save(sesion);
            for (Usuario estudiante : grupo.estudiantes) {
                autorizar(sesion, estudiante);
                bitacora.avisar(estudiante, "CORREO", "Convocatoria: " + caso.nombre,
                        (entrada.mensaje() == null ? "" : entrada.mensaje() + " ")
                                + "Tu código de acceso es " + sesion.codigo + ". Inicio: " + inicio + ".");
            }
            bitacora.auditar(docente, "AGENDAR", caso.nombre);
            return mapeador.sesion(sesion);
        }
    }

    @Transactional(readOnly = true)
    public List<Contratos.SesionVista> sesionesDocente(Usuario docente) {
        proxy.revisarGrupo(docente);
        return sesiones.findByGrupo_Docente_IdOrderByIdDesc(docente.id).stream().map(mapeador::sesion).toList();
    }

    @Transactional
    public Contratos.SesionVista iniciar(Usuario docente, Long id) {
        proxy.revisarGrupo(docente);
        SesionCaso sesion = propia(docente, id);
        estadoDe(sesion).iniciar(sesion);
        bitacora.auditar(docente, "INICIAR_SESION", sesion.codigo);
        return mapeador.sesion(sesion);
    }

    @Transactional
    public Contratos.SesionVista finalizar(Usuario docente, Long id) {
        proxy.revisarGrupo(docente);
        SesionCaso sesion = propia(docente, id);
        new CierrePorDocente(participacionServicio, participaciones, bitacora).cerrar(sesion);
        bitacora.auditar(docente, "FINALIZAR_SESION", sesion.codigo);
        return mapeador.sesion(sesion);
    }

    @Transactional
    public void cerrarVencidas() {
        LocalDateTime ahora = LocalDateTime.now();
        for (SesionCaso sesion : sesiones.findByCierreRealIsNull()) {
            if (sesion.fin != null && !sesion.fin.isAfter(ahora)) {
                new CierrePorFecha(participacionServicio, participaciones, bitacora).cerrar(sesion);
            }
        }
    }

    private Grupo propio(Usuario docente, Long id) {
        Grupo grupo = grupos.findById(id).orElseThrow(() -> new NoEncontrado("Grupo no encontrado."));
        if (!grupo.esDirigidoPor(docente)) throw new AccesoDenegado("El grupo no está a tu cargo.");
        return grupo;
    }

    private SesionCaso propia(Usuario docente, Long id) {
        SesionCaso sesion = sesiones.findById(id).orElseThrow(() -> new NoEncontrado("Sesión no encontrada."));
        if (!sesion.grupo.esDirigidoPor(docente)) throw new AccesoDenegado("La sesión no está a tu cargo.");
        return sesion;
    }

    private void autorizar(SesionCaso sesion, Usuario estudiante) {
        if (autorizados.existsBySesionIdAndEstudianteId(sesion.id, estudiante.id)) return;
        SesionEstudiante fila = new SesionEstudiante();
        fila.sesionId = sesion.id;
        fila.estudianteId = estudiante.id;
        fila.grupoId = sesion.grupo.id;
        autorizados.save(fila);
    }

    private static LocalDateTime parse(String valor) {
        if (valor == null || valor.isBlank()) throw new ReglaNegocio("INV-18: indica inicio y fin.");
        String texto = valor.trim();
        if (texto.length() == 16) texto = texto + ":00";
        return LocalDateTime.parse(texto, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    record ContextoAgenda(Usuario docente, Caso caso, Grupo grupo, LocalDateTime inicio, LocalDateTime fin,
                          int tiempoMaxMin, int toleranciaMin) {}

    interface Manejador {
        Manejador enlazar(Manejador siguiente);
        void validar(ContextoAgenda contexto);
    }

    static abstract class PasoAgenda implements Manejador {
        private Manejador siguiente;
        public Manejador enlazar(Manejador proximo) { this.siguiente = proximo; return proximo; }
        public final void validar(ContextoAgenda contexto) {
            comprobar(contexto);
            if (siguiente != null) siguiente.validar(contexto);
        }
        abstract void comprobar(ContextoAgenda contexto);
    }

    static class CadenaAgendamiento {
        void validar(ContextoAgenda contexto) {
            PasoAgenda caso = new ValidadorCasoPublicado();
            PasoAgenda grupo = new ValidadorGrupoDelProfesor();
            PasoAgenda fechas = new ValidadorFechas();
            PasoAgenda tiempo = new ValidadorTiempoMaximo();
            PasoAgenda tolerancia = new ValidadorTolerancia();
            caso.enlazar(grupo).enlazar(fechas).enlazar(tiempo).enlazar(tolerancia);
            caso.validar(contexto);
        }
    }

    static class ValidadorCasoPublicado extends PasoAgenda {
        void comprobar(ContextoAgenda c) {
            if (c.caso.estado != Catalogo.EstadoCaso.PUBLICADO) {
                throw new ReglaNegocio("INV-17: el caso debe estar publicado para agendarlo.");
            }
        }
    }

    static class ValidadorGrupoDelProfesor extends PasoAgenda {
        void comprobar(ContextoAgenda c) {
            if (!c.grupo.esDirigidoPor(c.docente)) {
                throw new ReglaNegocio("INV-17: el grupo debe estar a cargo del docente.");
            }
            if (c.grupo.estudiantes.isEmpty()) {
                throw new ReglaNegocio("RF-04: asocia al menos un estudiante autorizado.");
            }
        }
    }

    static class ValidadorFechas extends PasoAgenda {
        void comprobar(ContextoAgenda c) {
            if (c.fin == null || c.inicio == null || !c.fin.isAfter(c.inicio)) {
                throw new ReglaNegocio("INV-18: el fin debe ser posterior al inicio.");
            }
        }
    }

    static class ValidadorTiempoMaximo extends PasoAgenda {
        void comprobar(ContextoAgenda c) {
            if (c.tiempoMaxMin <= 0) throw new ReglaNegocio("INV-18: el tiempo máximo debe ser mayor que cero.");
            if (c.caso.tiempoEstimadoMin != null && c.tiempoMaxMin > c.caso.tiempoEstimadoMin) {
                throw new ReglaNegocio("RF-18: el tiempo máximo no puede superar el estimado del caso.");
            }
        }
    }

    static class ValidadorTolerancia extends PasoAgenda {
        void comprobar(ContextoAgenda c) {
            if (c.toleranciaMin < 0) throw new ReglaNegocio("INV-18: la tolerancia no puede ser negativa.");
        }
    }

    abstract static class FabricaCodigo {
        String emitir(SesionRepo repo) {
            for (int i = 0; i < 8; i++) {
                String codigo = crear();
                if (repo.findByCodigoIgnoreCase(codigo).isEmpty()) return codigo;
            }
            throw new ReglaNegocio("No fue posible emitir un código único.");
        }
        abstract String crear();
    }

    static class FabricaCodigoAlfanumerico extends FabricaCodigo {
        @Override String crear() {
            String alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
            StringBuilder codigo = new StringBuilder("DL-");
            for (int i = 0; i < 4; i++) codigo.append(alfabeto.charAt(ThreadLocalRandom.current().nextInt(alfabeto.length())));
            return codigo.toString();
        }
    }

    /** State (RF-19): iniciar solo lo acepta una sesión sin iniciar. */
    interface EstadoSesion { void iniciar(SesionCaso sesion); }

    static EstadoSesion estadoDe(SesionCaso sesion) {
        return switch (sesion.estado()) {
            case EN_CURSO -> new EnCurso();
            case FINALIZADO -> new Finalizado();
            default -> new SinIniciar();
        };
    }

    static class SinIniciar implements EstadoSesion {
        public void iniciar(SesionCaso sesion) { sesion.inicioReal = LocalDateTime.now(); }
    }

    static class EnCurso implements EstadoSesion {
        public void iniciar(SesionCaso sesion) {
            throw new ReglaNegocio("RF-19: solo se inicia una sesión sin iniciar.");
        }
    }

    static class Finalizado implements EstadoSesion {
        public void iniciar(SesionCaso sesion) {
            throw new ReglaNegocio("RF-19: solo se inicia una sesión sin iniciar.");
        }
    }

    /** Template Method (RF-24): el cierre siempre evalúa con la misma secuencia. */
    abstract static class CierreSesion {
        private final ParticipacionServicio participacionesSrv;
        private final ParticipacionRepo participaciones;
        private final BitacoraServicio bitacora;

        CierreSesion(ParticipacionServicio participacionesSrv, ParticipacionRepo participaciones, BitacoraServicio bitacora) {
            this.participacionesSrv = participacionesSrv;
            this.participaciones = participaciones;
            this.bitacora = bitacora;
        }

        final void cerrar(SesionCaso sesion) {
            if (!puedeCerrar(sesion)) return;
            if (sesion.estado() == Catalogo.EstadoSesion.FINALIZADO) return;
            for (Usuario estudiante : new ArrayList<>(sesion.grupo.estudiantes)) {
                List<Participacion> propias = participaciones.findByEstudianteIdAndSesionId(estudiante.id, sesion.id);
                Participacion enCurso = propias.stream().filter(Participacion::enCurso).findFirst().orElse(null);
                if (enCurso != null) {
                    participacionesSrv.cerrarPorTiempo(enCurso);
                } else if (propias.stream().noneMatch(p -> p.finalizada() || p.ausente())) {
                    Participacion ausencia = new Participacion();
                    ausencia.estudiante = estudiante;
                    ausencia.sesion = sesion;
                    ausencia.casoId = sesion.caso.id;
                    ausencia.numeroIntento = 1;
                    ausencia.fijarNota(new BigDecimal("0.0"));
                    participaciones.save(ausencia);
                }
            }
            sesion.cierreReal = LocalDateTime.now();
        }

        abstract boolean puedeCerrar(SesionCaso sesion);
    }

    static class CierrePorDocente extends CierreSesion {
        CierrePorDocente(ParticipacionServicio a, ParticipacionRepo b, BitacoraServicio c) { super(a, b, c); }
        boolean puedeCerrar(SesionCaso sesion) {
            if (sesion.estado() != Catalogo.EstadoSesion.EN_CURSO) {
                throw new ReglaNegocio("RF-19: solo se finaliza una sesión en curso.");
            }
            return true;
        }
    }

    static class CierrePorFecha extends CierreSesion {
        CierrePorFecha(ParticipacionServicio a, ParticipacionRepo b, BitacoraServicio c) { super(a, b, c); }
        boolean puedeCerrar(SesionCaso sesion) {
            return sesion.estado() == Catalogo.EstadoSesion.EN_CURSO
                    && sesion.fin != null
                    && !sesion.fin.isAfter(LocalDateTime.now());
        }
    }
}
