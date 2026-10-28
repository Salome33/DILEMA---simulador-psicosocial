package com.dilema.servicio;

import com.dilema.dto.Contratos;
import com.dilema.modelo.*;
import com.dilema.repo.ParticipacionRepo;
import com.dilema.repo.SesionEstudianteRepo;
import com.dilema.repo.SesionRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ParticipacionServicio {
    private final ParticipacionRepo participaciones;
    private final SesionRepo sesiones;
    private final SesionEstudianteRepo autorizados;
    private final ProxyOperacion proxy;
    private final Mapeador mapeador;
    private final ConcurrentHashMap<Long, List<Paso>> historiales = new ConcurrentHashMap<>();

    public ParticipacionServicio(ParticipacionRepo participaciones, SesionRepo sesiones, SesionEstudianteRepo autorizados,
                                 ProxyOperacion proxy, Mapeador mapeador) {
        this.participaciones = participaciones;
        this.sesiones = sesiones;
        this.autorizados = autorizados;
        this.proxy = proxy;
        this.mapeador = mapeador;
    }

    @Transactional(readOnly = true)
    public Contratos.PanelEstudiante panel(Usuario estudiante) {
        proxy.participar(estudiante);
        List<SesionCaso> propias = sesiones.delEstudiante(estudiante.id);
        List<Contratos.SesionVista> sesionesVista = propias.stream().map(sesion -> {
            Participacion ultima = participaciones.findByEstudianteIdAndSesionId(estudiante.id, sesion.id).stream()
                    .max(Comparator.comparing(p -> p.id)).orElse(null);
            return mapeador.sesion(sesion, ultima);
        }).toList();
        List<Contratos.Aviso> avisos = new ArrayList<>();
        for (SesionCaso sesion : propias) {
            avisos.add(new Contratos.Aviso(sesion.id, "CORREO", "Caso asignado",
                    sesion.caso.nombre + ". " + (sesion.mensaje == null ? "" : sesion.mensaje),
                    false, sesion.inicio.toString()));
            if (sesion.estado() == Catalogo.EstadoSesion.EN_CURSO) {
                avisos.add(new Contratos.Aviso(sesion.id + 10000, "SISTEMA", "Recordatorio de vencimiento",
                        "La convocatoria sigue en curso. Tiempo máximo: " + sesion.tiempoMaxMin + " minutos.",
                        false, sesion.inicioReal == null ? sesion.inicio.toString() : sesion.inicioReal.toString()));
            }
        }
        for (Participacion participacion : participaciones.findByEstudianteIdOrderByIdDesc(estudiante.id)) {
            new SujetoParticipacion().notificar(participacion, avisos);
        }
        return new Contratos.PanelEstudiante(sesionesVista, avisos, avisos.size());
    }

    @Transactional
    public void leerAviso(Usuario estudiante, Long id) {
        proxy.participar(estudiante);
    }

    @Transactional
    public Contratos.ParticipacionVista iniciar(Usuario estudiante, Long sesionId) {
        proxy.participar(estudiante);
        SesionCaso sesion = sesiones.findById(sesionId).orElseThrow(() -> new NoEncontrado("Sesión no encontrada."));
        if (!autorizados.existsBySesionIdAndEstudianteId(sesion.id, estudiante.id)) {
            throw new AccesoDenegado("No estás autorizado en esta convocatoria.");
        }
        new ProxyParticipacion().sesionAbierta(sesion);
        List<Participacion> propias = participaciones.findByEstudianteIdAndSesionId(estudiante.id, sesion.id);
        Participacion enCurso = propias.stream().filter(Participacion::enCurso).findFirst().orElse(null);
        if (enCurso != null) return vista(enCurso, false);
        Participacion previa = propias.stream().filter(Participacion::finalizada).max(Comparator.comparing(p -> p.id)).orElse(null);
        new ProxyParticipacion().puedeRepetir(previa);
        if (previa == null) new ProxyParticipacion().ingresoTardio(sesion, LocalDateTime.now());
        Participacion nueva = new Participacion();
        nueva.estudiante = estudiante;
        nueva.sesion = sesion;
        nueva.casoId = sesion.caso.id;
        nueva.numeroIntento = previa == null ? 1 : previa.numeroIntento + 1;
        nueva.inicio = LocalDateTime.now();
        participaciones.save(nueva);
        return vista(nueva, false);
    }

    @Transactional(readOnly = true)
    public Contratos.ParticipacionVista ver(Usuario usuario, Long id) {
        Participacion participacion = propiaODocente(usuario, id);
        boolean revelar = new ProxyResultado().revelar(usuario, participacion);
        return vista(participacion, revelar);
    }

    /** Command (RF-10): el cambio de opción se puede deshacer durante el intento. */
    @Transactional
    public Contratos.ParticipacionVista responder(Usuario estudiante, Long id, Long preguntaId, Long opcionId) {
        proxy.participar(estudiante);
        Participacion participacion = enCurso(estudiante, id);
        if (tiempoAgotado(participacion)) return vista(participacion, true);
        Pregunta pregunta = buscarPregunta(participacion.sesion.caso, preguntaId);
        Opcion opcion = pregunta.opciones.stream().filter(o -> o.id.equals(opcionId)).findFirst()
                .orElseThrow(() -> new ReglaNegocio("La opción no pertenece a la pregunta."));
        Respuesta actual = participacion.respuestas.stream().filter(r -> r.pregunta.id.equals(preguntaId)).findFirst().orElse(null);
        Long antes = actual == null ? null : actual.opcion.id;
        new ComandoRespuesta(preguntaId, antes, opcionId).ejecutar(participacion, pregunta, opcion, actual);
        historiales.computeIfAbsent(participacion.id, k -> new ArrayList<>()).add(new Paso(preguntaId, antes, opcionId));
        return vista(participacion, false);
    }

    @Transactional
    public Contratos.ParticipacionVista deshacer(Usuario estudiante, Long id) {
        proxy.participar(estudiante);
        Participacion participacion = enCurso(estudiante, id);
        if (tiempoAgotado(participacion)) return vista(participacion, true);
        List<Paso> pila = historiales.getOrDefault(participacion.id, new ArrayList<>());
        Paso paso = new HistorialRespuestas().deshacer(pila);
        new ComandoRespuesta(paso.preguntaId(), paso.antes(), paso.despues()).revertir(participacion, this);
        return vista(participacion, false);
    }

    @Transactional
    public Contratos.ParticipacionVista finalizar(Usuario estudiante, Long id) {
        proxy.participar(estudiante);
        Participacion participacion = enCurso(estudiante, id);
        cerrar(participacion);
        return vista(participacion, true);
    }

    @Transactional
    public void cerrarPorTiempo(Participacion participacion) {
        if (participacion.enCurso()) cerrar(participacion);
    }

    @Transactional
    public void retroalimentar(Usuario docente, Long id, String texto) {
        proxy.revisarGrupo(docente);
        Participacion participacion = participaciones.findById(id).orElseThrow(() -> new NoEncontrado("Participación no encontrada."));
        if (!participacion.sesion.caso.docente.id.equals(docente.id)) throw new AccesoDenegado("Solo el docente del caso puede valorarla.");
        new EstadoValoracion().aceptar(participacion);
        if (texto == null || texto.isBlank()) throw new ReglaNegocio("Escribe la valoración general.");
        if (participacion.retroalimentacionDocente == null) {
            RetroalimentacionDocente comentario = new RetroalimentacionDocente();
            comentario.participacion = participacion;
            participacion.retroalimentacionDocente = comentario;
        }
        participacion.retroalimentacionDocente.comentario = texto.trim();
    }

    @Transactional
    public void autorizarRepeticion(Usuario docente, Long id) {
        proxy.crearCaso(docente);
        Participacion participacion = participaciones.findById(id).orElseThrow(() -> new NoEncontrado("Participación no encontrada."));
        if (!participacion.sesion.caso.docente.id.equals(docente.id)) {
            throw new AccesoDenegado("RF-25: solo el docente creador del caso autoriza la repetición.");
        }
        if (participacion.sesion.estado() == Catalogo.EstadoSesion.FINALIZADO) {
            throw new ReglaNegocio("RF-25: no se autoriza repetición en una sesión finalizada.");
        }
        if (!participacion.finalizada()) throw new ReglaNegocio("Solo se autoriza repetir un intento ya terminado.");
        if (participacion.autorizacion == null) {
            AutorizacionRepeticion permiso = new AutorizacionRepeticion();
            permiso.participacion = participacion;
            permiso.fechaAutorizacion = LocalDateTime.now();
            participacion.autorizacion = permiso;
        }
    }

    @Transactional(readOnly = true)
    public List<Contratos.FilaSeguimiento> seguimiento(Usuario docente, Long sesionId) {
        proxy.revisarGrupo(docente);
        SesionCaso sesion = sesiones.findById(sesionId).orElseThrow(() -> new NoEncontrado("Sesión no encontrada."));
        if (!sesion.grupo.esDirigidoPor(docente)) throw new AccesoDenegado("La sesión no está a tu cargo.");
        List<Contratos.FilaSeguimiento> filas = new ArrayList<>();
        IteradorCohorte iterador = new IteradorCohorte(sesion.grupo.estudiantes);
        while (iterador.hasNext()) {
            Usuario estudiante = iterador.next();
            Participacion ultima = participaciones.findByEstudianteIdAndSesionId(estudiante.id, sesion.id).stream()
                    .max(Comparator.comparing(p -> p.id)).orElse(null);
            if (ultima == null) {
                Double nota = sesion.estado() == Catalogo.EstadoSesion.FINALIZADO ? 0.0 : null;
                filas.add(new Contratos.FilaSeguimiento(null, estudiante.id, estudiante.nombre, estudiante.email,
                        sesion.estado() == Catalogo.EstadoSesion.FINALIZADO ? "AUSENTE" : "SIN_INICIAR", nota, ""));
            } else {
                filas.add(new Contratos.FilaSeguimiento(ultima.id, estudiante.id, estudiante.nombre, estudiante.email,
                        ultima.estadoNombre(), ultima.nota(), ultima.comentarioDocente()));
            }
        }
        return filas;
    }

    @Transactional(readOnly = true)
    public String reporte(Usuario docente, Long sesionId) {
        List<Contratos.FilaSeguimiento> filas = seguimiento(docente, sesionId);
        return new ReporteCsv().generar(filas);
    }

    private void cerrar(Participacion participacion) {
        participacion.fin = LocalDateTime.now();
        participacion.fijarNota(calcularNota(participacion));
    }

    private boolean tiempoAgotado(Participacion participacion) {
        long usados = Duration.between(participacion.inicio, LocalDateTime.now()).getSeconds();
        if (usados >= participacion.sesion.tiempoMaxMin * 60L) {
            cerrar(participacion);
            return true;
        }
        return false;
    }

    private Participacion enCurso(Usuario estudiante, Long id) {
        Participacion participacion = participaciones.findById(id).orElseThrow(() -> new NoEncontrado("Participación no encontrada."));
        if (!participacion.estudiante.id.equals(estudiante.id)) throw new AccesoDenegado("Esta participación no es tuya.");
        if (!participacion.enCurso()) throw new ReglaNegocio("La participación ya finalizó.");
        return participacion;
    }

    private Participacion propiaODocente(Usuario usuario, Long id) {
        Participacion participacion = participaciones.findById(id).orElseThrow(() -> new NoEncontrado("Participación no encontrada."));
        if (usuario.rol == Catalogo.Rol.ESTUDIANTE && participacion.estudiante.id.equals(usuario.id)) return participacion;
        if (usuario.rol == Catalogo.Rol.PROFESOR && participacion.sesion.caso.docente.id.equals(usuario.id)) return participacion;
        throw new AccesoDenegado("No puedes consultar esta participación.");
    }

    BigDecimal calcularNota(Participacion participacion) {
        return new CalculadoraPorPesos().calcular(participacion);
    }

    private boolean acierto(Participacion participacion, Pregunta pregunta) {
        return participacion.respuestas.stream().anyMatch(r -> r.pregunta.id.equals(pregunta.id) && r.opcion.esCorrecta);
    }

    private List<Pregunta> preguntas(Caso caso) {
        List<Pregunta> todas = new ArrayList<>();
        for (Escenario escenario : caso.escenarios) todas.addAll(escenario.preguntas);
        return todas;
    }

    private Pregunta buscarPregunta(Caso caso, Long id) {
        return preguntas(caso).stream().filter(p -> p.id.equals(id)).findFirst()
                .orElseThrow(() -> new ReglaNegocio("La pregunta no pertenece a este caso."));
    }

    private Contratos.ParticipacionVista vista(Participacion participacion, boolean revelar) {
        List<Contratos.PreguntaJuego> preguntas = new ArrayList<>();
        for (Escenario escenario : participacion.sesion.caso.escenarios) {
            for (Pregunta pregunta : escenario.preguntas) {
                Long seleccion = participacion.respuestas.stream().filter(r -> r.pregunta.id.equals(pregunta.id))
                        .map(r -> r.opcion.id).findFirst().orElse(null);
                List<Contratos.OpcionJuego> opciones = pregunta.opciones.stream()
                        .map(o -> new Contratos.OpcionJuego(o.id, o.descripcion,
                                revelar ? o.esCorrecta : null,
                                revelar && o.retroalimentacion != null ? o.retroalimentacion.explicacion : null,
                                null))
                        .toList();
                String criterio = pregunta.criterio == null ? "" : pregunta.criterio.criterio;
                preguntas.add(new Contratos.PreguntaJuego(pregunta.id, escenario.titulo, escenario.contexto,
                        pregunta.enunciado, pregunta.pista, criterio, seleccion, opciones));
            }
        }
        LocalDateTime hasta = participacion.fin == null ? LocalDateTime.now() : participacion.fin;
        long usados = participacion.inicio == null ? 0 : Math.max(0, Duration.between(participacion.inicio, hasta).getSeconds());
        int total = preguntas.size();
        int progreso = total == 0 ? 0 : (int) Math.round(participacion.respuestas.size() * 100.0 / total);
        return new Contratos.ParticipacionVista(participacion.id, participacion.sesion.id, participacion.estadoNombre(),
                participacion.sesion.caso.nombre, progreso, total,
                participacion.sesion.tiempoMaxMin * 60, usados, participacion.nota(),
                participacion.comentarioDocente(), participacion.estudiante.nombre, preguntas);
    }

    public record Paso(Long preguntaId, Long antes, Long despues) {}

    /** Proxy de protección (RF-22 y RF-25). */
    class ProxyParticipacion {
        void sesionAbierta(SesionCaso sesion) {
            if (sesion.estado() == Catalogo.EstadoSesion.FINALIZADO) {
                throw new ReglaNegocio("RF-25: la sesión ya finalizó y no admite participación.");
            }
            if (sesion.estado() != Catalogo.EstadoSesion.EN_CURSO) {
                throw new ReglaNegocio("La sesión todavía no está en curso.");
            }
        }
        void ingresoTardio(SesionCaso sesion, LocalDateTime ahora) {
            if (!sesion.permiteIngreso(ahora)) {
                throw new ReglaNegocio("RF-22: ya pasó el tiempo definido por el docente para iniciar.");
            }
        }
        void puedeRepetir(Participacion previa) {
            if (previa != null && previa.autorizacion == null) {
                throw new ReglaNegocio("RF-25: ya registraste una participación. Solo el docente creador puede autorizar otra.");
            }
        }
    }

    /** Proxy (RF-14): la explicación solo se revela al terminar. */
    class ProxyResultado {
        boolean revelar(Usuario usuario, Participacion participacion) {
            return usuario.rol != Catalogo.Rol.ESTUDIANTE || participacion.finalizada() || participacion.ausente();
        }
    }

    /** Command (RF-10). */
    class ComandoRespuesta {
        private final Long preguntaId;
        private final Long antes;
        private final Long despues;
        ComandoRespuesta(Long preguntaId, Long antes, Long despues) {
            this.preguntaId = preguntaId;
            this.antes = antes;
            this.despues = despues;
        }
        void ejecutar(Participacion participacion, Pregunta pregunta, Opcion opcion, Respuesta actual) {
            if (actual == null) {
                Respuesta respuesta = new Respuesta();
                respuesta.participacion = participacion;
                respuesta.pregunta = pregunta;
                respuesta.casoId = participacion.casoId;
                respuesta.opcion = opcion;
                participacion.respuestas.add(respuesta);
            } else {
                actual.opcion = opcion;
            }
        }
        void revertir(Participacion participacion, ParticipacionServicio servicio) {
            Respuesta actual = participacion.respuestas.stream().filter(r -> r.pregunta.id.equals(preguntaId)).findFirst().orElse(null);
            if (antes == null) {
                if (actual != null) participacion.respuestas.remove(actual);
            } else if (actual != null) {
                Pregunta pregunta = servicio.buscarPregunta(participacion.sesion.caso, preguntaId);
                actual.opcion = pregunta.opciones.stream().filter(o -> o.id.equals(antes)).findFirst()
                        .orElseThrow(() -> new ReglaNegocio("No se pudo restaurar la opción anterior."));
            }
        }
    }

    class HistorialRespuestas {
        Paso deshacer(List<Paso> pila) {
            if (pila.isEmpty()) throw new ReglaNegocio("No hay respuestas para deshacer.");
            return pila.remove(pila.size() - 1);
        }
    }

    /** State (RF-26): la valoración general solo existe al finalizar. */
    class EstadoValoracion {
        void aceptar(Participacion participacion) {
            if (!participacion.finalizada()) {
                throw new ReglaNegocio("RF-26: la valoración general solo se registra cuando la participación finalizó.");
            }
        }
    }

    /** Strategy (RF-13): la nota es la suma de los pesos de las preguntas acertadas. */
    class CalculadoraPorPesos {
        BigDecimal calcular(Participacion participacion) {
            BigDecimal acum = BigDecimal.ZERO;
            for (Pregunta pregunta : preguntas(participacion.sesion.caso)) {
                if (pregunta.criterio == null || pregunta.criterio.peso == null) continue;
                if (acierto(participacion, pregunta)) acum = acum.add(pregunta.criterio.peso);
            }
            return acum.setScale(1, RoundingMode.HALF_UP);
        }
    }

    /** Observer (RF-11 y RF-27): la participación avisa nota y feedback sin guardar el aviso. */
    interface ObservadorAviso { void actualizar(Participacion participacion, List<Contratos.Aviso> destino); }

    class SujetoParticipacion {
        private final List<ObservadorAviso> observadores = List.of(new ObservadorNota(), new ObservadorFeedback());
        void notificar(Participacion participacion, List<Contratos.Aviso> destino) {
            for (ObservadorAviso observador : observadores) observador.actualizar(participacion, destino);
        }
    }

    class ObservadorNota implements ObservadorAviso {
        public void actualizar(Participacion participacion, List<Contratos.Aviso> destino) {
            if (participacion.nota() == null) return;
            destino.add(new Contratos.Aviso(participacion.id + 20000, "SISTEMA", "Nota del caso",
                    participacion.sesion.caso.nombre + ": " + participacion.nota() + " / 5,0.",
                    false, participacion.fin == null ? "" : participacion.fin.toString()));
        }
    }

    class ObservadorFeedback implements ObservadorAviso {
        public void actualizar(Participacion participacion, List<Contratos.Aviso> destino) {
            if (participacion.comentarioDocente() == null) return;
            destino.add(new Contratos.Aviso(participacion.id + 30000, "DOCENTE", "Feedback del docente",
                    participacion.comentarioDocente(), false, ""));
        }
    }

    /** Iterator (RF-15): recorre la cohorte de uno en uno. */
    class IteradorCohorte implements java.util.Iterator<Usuario> {
        private final List<Usuario> lista;
        private int indice = 0;
        IteradorCohorte(java.util.Collection<Usuario> estudiantes) {
            this.lista = estudiantes.stream().sorted(Comparator.comparing(u -> u.nombre)).toList();
        }
        public boolean hasNext() { return indice < lista.size(); }
        public Usuario next() { return lista.get(indice++); }
    }

    /** Strategy (RF-16). */
    class ReporteCsv {
        String generar(List<Contratos.FilaSeguimiento> filas) {
            StringBuilder csv = new StringBuilder("\uFEFFestudiante,email,estado,nota,retroalimentacion\n");
            for (Contratos.FilaSeguimiento fila : filas) {
                csv.append(esc(fila.estudiante())).append(',')
                        .append(esc(fila.email())).append(',')
                        .append(fila.estado()).append(',')
                        .append(fila.nota() == null ? "" : fila.nota()).append(',')
                        .append(esc(fila.retroalimentacion()))
                        .append('\n');
            }
            return csv.toString();
        }
    }

    private static String esc(String valor) {
        if (valor == null) return "";
        return "\"" + valor.replace("\"", "'") + "\"";
    }
}
