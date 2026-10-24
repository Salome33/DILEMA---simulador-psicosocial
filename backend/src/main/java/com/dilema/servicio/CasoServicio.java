package com.dilema.servicio;

import com.dilema.dto.Contratos;
import com.dilema.modelo.*;
import com.dilema.repo.CasoRepo;
import com.dilema.repo.EscenarioPersonajeRepo;
import com.dilema.repo.SesionRepo;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class CasoServicio {
    private final CasoRepo casos;
    private final SesionRepo sesiones;
    private final EscenarioPersonajeRepo vinculos;
    private final ProxyOperacion proxy;
    private final Mapeador mapeador;
    private final EntityManager em;

    public CasoServicio(CasoRepo casos, SesionRepo sesiones, EscenarioPersonajeRepo vinculos,
                        ProxyOperacion proxy, Mapeador mapeador, EntityManager em) {
        this.casos = casos;
        this.sesiones = sesiones;
        this.vinculos = vinculos;
        this.proxy = proxy;
        this.mapeador = mapeador;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public List<Contratos.CasoResumen> listar(Usuario docente) {
        proxy.revisarGrupo(docente);
        return casos.findByDocenteIdOrderByIdDesc(docente.id).stream().map(mapeador::resumen).toList();
    }

    @Transactional(readOnly = true)
    public Contratos.CasoVista obtener(Usuario docente, Long id) {
        proxy.revisarGrupo(docente);
        Caso caso = propio(docente, id);
        return mapeador.caso(caso, vinculos.findByCasoId(caso.id));
    }

    @Transactional
    public Contratos.CasoVista crear(Usuario docente, Contratos.CasoIn entrada) {
        proxy.crearCaso(docente);
        Caso caso = new ConstructorCasoNuevo().construir(docente, entrada);
        casos.save(caso);
        em.flush();
        fijarCasoId(caso);
        enlazar(caso, entrada);
        return mapeador.caso(caso, vinculos.findByCasoId(caso.id));
    }

    @Transactional
    public Contratos.CasoVista actualizar(Usuario docente, Long id, Contratos.CasoIn entrada) {
        proxy.crearCaso(docente);
        Caso caso = propio(docente, id);
        estadoDe(caso).editar();
        vinculos.deleteByCasoId(caso.id);
        caso.escenarios.clear();
        caso.personajes.clear();
        em.flush();
        new ConstructorCasoNuevo().copiar(caso, entrada);
        em.flush();
        fijarCasoId(caso);
        enlazar(caso, entrada);
        return mapeador.caso(caso, vinculos.findByCasoId(caso.id));
    }

    @Transactional
    public Contratos.CasoVista publicar(Usuario docente, Long id) {
        proxy.crearCaso(docente);
        Caso caso = propio(docente, id);
        estadoDe(caso).publicar(caso, validar(caso));
        return mapeador.caso(caso, vinculos.findByCasoId(caso.id));
    }

    @Transactional
    public Contratos.CasoVista archivar(Usuario docente, Long id) {
        proxy.crearCaso(docente);
        Caso caso = propio(docente, id);
        estadoDe(caso).archivar(caso);
        return mapeador.caso(caso, vinculos.findByCasoId(caso.id));
    }

    @Transactional
    public Contratos.CasoVista despublicar(Usuario docente, Long id) {
        proxy.crearCaso(docente);
        Caso caso = propio(docente, id);
        boolean usado = sesiones.findAll().stream().anyMatch(s -> s.caso.id.equals(caso.id))
                || em.createQuery("select count(p) from Participacion p where p.casoId = :id", Long.class)
                .setParameter("id", caso.id).getSingleResult() > 0;
        estadoDe(caso).despublicar(caso, usado);
        return mapeador.caso(caso, vinculos.findByCasoId(caso.id));
    }

    /** INV-12: la copia nace en borrador, sin sesiones ni participaciones. */
    @Transactional
    public Contratos.CasoVista duplicar(Usuario docente, Long id) {
        proxy.crearCaso(docente);
        Caso origen = propio(docente, id);
        Caso copia = new Caso();
        copia.docente = docente;
        String nombre = origen.nombre + " (copia)";
        copia.nombre = nombre.length() > 200 ? origen.nombre : nombre;
        copia.desarrolloSituacional = origen.desarrolloSituacional;
        copia.contexto = origen.contexto;
        copia.tiempoEstimadoMin = origen.tiempoEstimadoMin;
        copia.estado = Catalogo.EstadoCaso.BORRADOR;
        for (Escenario escenario : origen.escenarios) {
            Escenario nuevo = new Escenario();
            nuevo.caso = copia;
            nuevo.orden = escenario.orden;
            nuevo.titulo = escenario.titulo;
            nuevo.contexto = escenario.contexto;
            for (Pregunta pregunta : escenario.preguntas) {
                Pregunta preguntaNueva = new Pregunta();
                preguntaNueva.escenario = nuevo;
                preguntaNueva.casoId = 0L;
                preguntaNueva.orden = pregunta.orden;
                preguntaNueva.enunciado = pregunta.enunciado;
                preguntaNueva.pista = pregunta.pista;
                if (pregunta.criterio != null) {
                    CriterioRubrica criterio = new CriterioRubrica();
                    criterio.pregunta = preguntaNueva;
                    criterio.criterio = pregunta.criterio.criterio;
                    criterio.peso = pregunta.criterio.peso;
                    preguntaNueva.criterio = criterio;
                }
                for (Opcion opcion : pregunta.opciones) {
                    Opcion opcionNueva = new Opcion();
                    opcionNueva.pregunta = preguntaNueva;
                    opcionNueva.orden = opcion.orden;
                    opcionNueva.descripcion = opcion.descripcion;
                    opcionNueva.esCorrecta = opcion.esCorrecta;
                    if (opcion.retroalimentacion != null) {
                        Retroalimentacion retro = new Retroalimentacion();
                        retro.opcion = opcionNueva;
                        retro.explicacion = opcion.retroalimentacion.explicacion;
                        opcionNueva.retroalimentacion = retro;
                    }
                    preguntaNueva.opciones.add(opcionNueva);
                }
                nuevo.preguntas.add(preguntaNueva);
            }
            copia.escenarios.add(nuevo);
        }
        for (Personaje personaje : origen.personajes) {
            Personaje nuevo = new Personaje();
            nuevo.caso = copia;
            nuevo.nombre = personaje.nombre;
            nuevo.descripcion = personaje.descripcion;
            copia.personajes.add(nuevo);
        }
        casos.save(copia);
        em.flush();
        fijarCasoId(copia);
        copiarVinculos(origen, copia);
        return mapeador.caso(copia, vinculos.findByCasoId(copia.id));
    }

    private void copiarVinculos(Caso origen, Caso copia) {
        for (EscenarioPersonaje vinculo : vinculos.findByCasoId(origen.id)) {
            Escenario escenarioOrigen = origen.escenarios.stream().filter(e -> e.id.equals(vinculo.escenarioId)).findFirst().orElse(null);
            Personaje personajeOrigen = origen.personajes.stream().filter(p -> p.id.equals(vinculo.personajeId)).findFirst().orElse(null);
            if (escenarioOrigen == null || personajeOrigen == null) continue;
            int indice = origen.personajes.indexOf(personajeOrigen);
            Escenario escenarioNuevo = copia.escenarios.stream().filter(e -> e.orden == escenarioOrigen.orden).findFirst().orElse(null);
            if (escenarioNuevo == null || indice < 0 || indice >= copia.personajes.size()) continue;
            EscenarioPersonaje nuevo = new EscenarioPersonaje();
            nuevo.escenarioId = escenarioNuevo.id;
            nuevo.personajeId = copia.personajes.get(indice).id;
            nuevo.casoId = copia.id;
            em.persist(nuevo);
        }
    }

    public Caso propio(Usuario docente, Long id) {
        Caso caso = casos.findById(id).orElseThrow(() -> new NoEncontrado("Caso no encontrado."));
        if (!caso.docente.id.equals(docente.id)) throw new AccesoDenegado("Este caso pertenece a otro docente.");
        return caso;
    }

    private void fijarCasoId(Caso caso) {
        for (Escenario escenario : caso.escenarios) {
            for (Pregunta pregunta : escenario.preguntas) pregunta.casoId = caso.id;
        }
    }

    private void enlazar(Caso caso, Contratos.CasoIn entrada) {
        List<Contratos.PersonajeIn> pedidos = entrada.personajes() == null ? List.of() : entrada.personajes();
        for (int i = 0; i < pedidos.size() && i < caso.personajes.size(); i++) {
            Personaje personaje = caso.personajes.get(i);
            List<Integer> ordenes = pedidos.get(i).escenarios() == null ? List.of() : pedidos.get(i).escenarios();
            for (Integer orden : ordenes) {
                Escenario escenario = caso.escenarios.stream().filter(e -> e.orden == orden).findFirst().orElse(null);
                if (escenario == null || escenario.id == null || personaje.id == null) continue;
                EscenarioPersonaje vinculo = new EscenarioPersonaje();
                vinculo.escenarioId = escenario.id;
                vinculo.personajeId = personaje.id;
                vinculo.casoId = caso.id;
                em.persist(vinculo);
            }
        }
    }

    /** Composite (RF-06): Caso, Escenario, Pregunta y Opción se validan como un solo árbol. */
    public List<String> validar(Caso caso) {
        List<String> errores = new ArrayList<>();
        new NodoCaso(caso).validar(errores);
        return errores;
    }

    private EstadoCaso estadoDe(Caso caso) {
        return switch (caso.estado) {
            case PUBLICADO -> new Publicado();
            case ARCHIVADO -> new Archivado();
            default -> new Borrador();
        };
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    /** State (RF-09): cada estado acepta o rechaza editar, publicar, despublicar y archivar. */
    interface EstadoCaso {
        void editar();
        void publicar(Caso caso, List<String> errores);
        void archivar(Caso caso);
        void despublicar(Caso caso, boolean usado);
    }

    class Borrador implements EstadoCaso {
        public void editar() {}
        public void publicar(Caso caso, List<String> errores) {
            if (!errores.isEmpty()) throw new ReglaNegocio(String.join(" ", errores));
            caso.estado = Catalogo.EstadoCaso.PUBLICADO;
        }
        public void archivar(Caso caso) {
            throw new ReglaNegocio("INV-11: un borrador se publica. Solo un caso publicado se archiva.");
        }
        public void despublicar(Caso caso, boolean usado) {
            throw new ReglaNegocio("Solo un caso publicado puede volver a borrador.");
        }
    }

    class Publicado implements EstadoCaso {
        public void editar() { throw new ReglaNegocio("RF-09: solo se edita un caso en borrador."); }
        public void publicar(Caso caso, List<String> errores) {
            throw new ReglaNegocio("RF-09: solo un borrador puede publicarse.");
        }
        public void archivar(Caso caso) { caso.estado = Catalogo.EstadoCaso.ARCHIVADO; }
        public void despublicar(Caso caso, boolean usado) {
            if (usado) throw new ReglaNegocio("INV-11: no se puede volver a borrador un caso que ya tiene sesiones o participaciones.");
            caso.estado = Catalogo.EstadoCaso.BORRADOR;
        }
    }

    class Archivado implements EstadoCaso {
        public void editar() { throw new ReglaNegocio("RF-09: solo se edita un caso en borrador."); }
        public void publicar(Caso caso, List<String> errores) {
            if (!errores.isEmpty()) throw new ReglaNegocio(String.join(" ", errores));
            caso.estado = Catalogo.EstadoCaso.PUBLICADO;
        }
        public void archivar(Caso caso) { throw new ReglaNegocio("El caso ya está archivado."); }
        public void despublicar(Caso caso, boolean usado) {
            throw new ReglaNegocio("Solo un caso publicado puede volver a borrador.");
        }
    }

    /** Composite (RF-06). */
    interface ElementoCaso { void validar(List<String> errores); }

    class NodoCaso implements ElementoCaso {
        private final Caso caso;
        NodoCaso(Caso caso) { this.caso = caso; }
        public void validar(List<String> errores) {
            if (vacio(caso.nombre)) errores.add("Falta el nombre del caso.");
            if (vacio(caso.desarrolloSituacional)) errores.add("Falta el desarrollo situacional.");
            if (vacio(caso.contexto)) errores.add("Falta el contexto.");
            if (caso.tiempoEstimadoMin == null || caso.tiempoEstimadoMin <= 0) errores.add("El tiempo estimado debe ser mayor que cero.");
            if (caso.escenarios.isEmpty()) errores.add("RF-05: registra al menos un escenario.");
            if (!caso.personajes.isEmpty()) {
                java.util.Set<Long> ligados = vinculos.findByCasoId(caso.id).stream()
                        .map(v -> v.personajeId).collect(java.util.stream.Collectors.toSet());
                for (Personaje personaje : caso.personajes) {
                    if (personaje.id == null || !ligados.contains(personaje.id)) {
                        errores.add("Cada personaje debe aparecer en al menos un escenario.");
                        break;
                    }
                }
            }
            BigDecimal suma = BigDecimal.ZERO;
            for (Escenario escenario : caso.escenarios) new NodoEscenario(escenario).validar(errores);
            for (Escenario escenario : caso.escenarios) {
                for (Pregunta pregunta : escenario.preguntas) {
                    if (pregunta.criterio != null && pregunta.criterio.peso != null) suma = suma.add(pregunta.criterio.peso);
                }
            }
            if (suma.compareTo(new BigDecimal("5.0")) != 0) {
                errores.add("INV-09: los pesos de la rúbrica deben sumar 5,0. Ahora suman " + suma.setScale(1, RoundingMode.HALF_UP) + ".");
            }
        }
    }

    class NodoEscenario implements ElementoCaso {
        private final Escenario escenario;
        NodoEscenario(Escenario escenario) { this.escenario = escenario; }
        public void validar(List<String> errores) {
            if (vacio(escenario.titulo) || vacio(escenario.contexto)) errores.add("Cada escenario necesita título y contexto.");
            if (escenario.preguntas.isEmpty()) errores.add("El escenario «" + escenario.titulo + "» no tiene preguntas.");
            for (Pregunta pregunta : escenario.preguntas) new NodoPregunta(pregunta).validar(errores);
        }
    }

    class NodoPregunta implements ElementoCaso {
        private final Pregunta pregunta;
        NodoPregunta(Pregunta pregunta) { this.pregunta = pregunta; }
        public void validar(List<String> errores) {
            if (vacio(pregunta.enunciado)) errores.add("Hay una pregunta sin enunciado.");
            if (pregunta.opciones.size() < 2) errores.add("Cada pregunta necesita al menos dos opciones.");
            long correctas = pregunta.opciones.stream().filter(o -> o.esCorrecta).count();
            if (correctas != 1) errores.add("Cada pregunta debe tener exactamente una opción correcta.");
            if (pregunta.criterio == null || vacio(pregunta.criterio.criterio) || pregunta.criterio.peso == null) {
                errores.add("RF-17: cada pregunta tiene su criterio y su peso.");
            }
            for (Opcion opcion : pregunta.opciones) new NodoOpcion(opcion).validar(errores);
        }
    }

    class NodoOpcion implements ElementoCaso {
        private final Opcion opcion;
        NodoOpcion(Opcion opcion) { this.opcion = opcion; }
        public void validar(List<String> errores) {
            if (vacio(opcion.descripcion) || opcion.retroalimentacion == null || vacio(opcion.retroalimentacion.explicacion)) {
                errores.add("Cada opción necesita texto y la explicación de por qué es correcta o incorrecta.");
            }
        }
    }

    /** Builder (RF-05, RF-07 y RF-08). */
    class ConstructorCasoNuevo {
        Caso construir(Usuario docente, Contratos.CasoIn entrada) {
            Caso caso = new Caso();
            caso.docente = docente;
            caso.estado = Catalogo.EstadoCaso.BORRADOR;
            caso.tiempoEstimadoMin = 1;
            copiar(caso, entrada);
            if (vacio(caso.nombre)) throw new ReglaNegocio("El borrador necesita al menos un nombre.");
            return caso;
        }

        void copiar(Caso caso, Contratos.CasoIn entrada) {
            caso.nombre = entrada.nombre();
            caso.desarrolloSituacional = entrada.desarrolloSituacional();
            caso.contexto = entrada.contexto();
            caso.tiempoEstimadoMin = entrada.tiempoEstimadoMin() == null ? 1 : entrada.tiempoEstimadoMin();
            int ordenEscenario = 0;
            for (Contratos.BloqueIn bloqueIn : entrada.bloques() == null ? List.<Contratos.BloqueIn>of() : entrada.bloques()) {
                Escenario escenario = new Escenario();
                escenario.caso = caso;
                escenario.titulo = bloqueIn.nombre();
                escenario.contexto = bloqueIn.contexto() == null ? "" : bloqueIn.contexto();
                escenario.orden = ordenEscenario++;
                int ordenPregunta = 0;
                for (Contratos.PreguntaIn preguntaIn : bloqueIn.preguntas() == null ? List.<Contratos.PreguntaIn>of() : bloqueIn.preguntas()) {
                    escenario.preguntas.add(new ConstructorPregunta().construir(caso, escenario, preguntaIn, ordenPregunta++));
                }
                caso.escenarios.add(escenario);
            }
            for (Contratos.PersonajeIn personajeIn : entrada.personajes() == null ? List.<Contratos.PersonajeIn>of() : entrada.personajes()) {
                if (personajeIn.nombre() == null || personajeIn.nombre().isBlank()) continue;
                Personaje personaje = new Personaje();
                personaje.caso = caso;
                personaje.nombre = personajeIn.nombre().trim();
                personaje.descripcion = personajeIn.descripcion();
                caso.personajes.add(personaje);
            }
        }
    }

    class ConstructorPregunta {
        Pregunta construir(Caso caso, Escenario escenario, Contratos.PreguntaIn entrada, int orden) {
            Pregunta pregunta = new Pregunta();
            pregunta.escenario = escenario;
            pregunta.casoId = caso.id == null ? 0L : caso.id;
            pregunta.enunciado = entrada.enunciado() == null ? "" : entrada.enunciado();
            pregunta.pista = entrada.pista();
            pregunta.orden = orden;
            CriterioRubrica criterio = new CriterioRubrica();
            criterio.pregunta = pregunta;
            criterio.criterio = entrada.criterio() == null ? "" : entrada.criterio();
            criterio.peso = BigDecimal.valueOf(entrada.peso() == null ? 0 : entrada.peso()).setScale(1, RoundingMode.HALF_UP);
            pregunta.criterio = criterio;
            int i = 0;
            for (Contratos.OpcionIn opcionIn : entrada.opciones() == null ? List.<Contratos.OpcionIn>of() : entrada.opciones()) {
                pregunta.opciones.add(new ConstructorOpcion().construir(pregunta, opcionIn, i++));
            }
            return pregunta;
        }
    }

    class ConstructorOpcion {
        Opcion construir(Pregunta pregunta, Contratos.OpcionIn entrada, int orden) {
            Opcion opcion = new Opcion();
            opcion.pregunta = pregunta;
            opcion.descripcion = entrada.texto() == null ? "" : entrada.texto();
            opcion.esCorrecta = entrada.correcta();
            opcion.orden = orden;
            String porque = entrada.explicacion() == null ? "" : entrada.explicacion().trim();
            if (entrada.fundamento() != null && !entrada.fundamento().isBlank()) {
                porque = (porque + " " + entrada.fundamento().trim()).trim();
            }
            Retroalimentacion retro = new Retroalimentacion();
            retro.opcion = opcion;
            retro.explicacion = porque;
            opcion.retroalimentacion = retro;
            return opcion;
        }
    }
}
