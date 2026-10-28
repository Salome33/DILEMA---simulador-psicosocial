package com.dilema.servicio;

import com.dilema.dto.Contratos;
import com.dilema.modelo.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class Mapeador {
    public Contratos.UsuarioVista usuario(Usuario u) {
        List<String> permisos = u.permisos.stream().map(permiso -> permiso.tipo).sorted().toList();
        return new Contratos.UsuarioVista(u.id, u.nombre, u.email, u.rol.name(), permisos);
    }

    public Contratos.PersonaVista persona(Usuario u) {
        return new Contratos.PersonaVista(u.id, u.nombre, u.email);
    }

    public Contratos.CasoVista caso(Caso caso, List<EscenarioPersonaje> vinculos) {
        return new Contratos.CasoVista(caso.id, caso.nombre, caso.desarrolloSituacional, caso.contexto,
                caso.tiempoEstimadoMin, false, "", caso.estado.name(), caso.docente.id,
                bloques(caso), rubrica(caso), personajes(caso, vinculos));
    }

    public Contratos.CasoResumen resumen(Caso caso) {
        int preguntas = caso.escenarios.stream().mapToInt(b -> b.preguntas.size()).sum();
        return new Contratos.CasoResumen(caso.id, caso.nombre, caso.estado.name(), caso.tiempoEstimadoMin,
                caso.escenarios.size(), preguntas);
    }

    public List<Contratos.BloqueIn> bloques(Caso caso) {
        List<Contratos.BloqueIn> salida = new ArrayList<>();
        for (Escenario escenario : caso.escenarios) {
            List<Contratos.PreguntaIn> preguntas = new ArrayList<>();
            for (Pregunta pregunta : escenario.preguntas) {
                List<Contratos.OpcionIn> opciones = pregunta.opciones.stream()
                        .map(o -> new Contratos.OpcionIn(o.id, o.descripcion, o.esCorrecta,
                                o.retroalimentacion == null ? "" : o.retroalimentacion.explicacion, ""))
                        .toList();
                String criterio = pregunta.criterio == null ? "" : pregunta.criterio.criterio;
                double peso = pregunta.criterio == null || pregunta.criterio.peso == null ? 0 : pregunta.criterio.peso.doubleValue();
                preguntas.add(new Contratos.PreguntaIn(pregunta.id, pregunta.enunciado, pregunta.pista, criterio, peso, opciones));
            }
            salida.add(new Contratos.BloqueIn(escenario.id, escenario.titulo, escenario.contexto, preguntas));
        }
        return salida;
    }

    public List<Contratos.CriterioIn> rubrica(Caso caso) {
        List<Contratos.CriterioIn> salida = new ArrayList<>();
        for (Escenario escenario : caso.escenarios) {
            for (Pregunta pregunta : escenario.preguntas) {
                if (pregunta.criterio == null) continue;
                salida.add(new Contratos.CriterioIn(pregunta.id, pregunta.criterio.criterio,
                        pregunta.criterio.peso.doubleValue(), escenario.titulo));
            }
        }
        return salida;
    }

    public List<Contratos.PersonajeIn> personajes(Caso caso, List<EscenarioPersonaje> vinculos) {
        List<Contratos.PersonajeIn> salida = new ArrayList<>();
        for (Personaje personaje : caso.personajes) {
            List<Integer> ordenes = new ArrayList<>();
            for (EscenarioPersonaje vinculo : vinculos) {
                if (!vinculo.personajeId.equals(personaje.id)) continue;
                caso.escenarios.stream().filter(e -> e.id.equals(vinculo.escenarioId)).findFirst()
                        .ifPresent(e -> ordenes.add(e.orden));
            }
            salida.add(new Contratos.PersonajeIn(personaje.id, personaje.nombre, personaje.descripcion, ordenes));
        }
        return salida;
    }

    public Contratos.GrupoVista grupo(Grupo grupo) {
        List<Contratos.PersonaVista> estudiantes = grupo.estudiantes.stream()
                .sorted(Comparator.comparing(u -> u.nombre))
                .map(this::persona)
                .toList();
        return new Contratos.GrupoVista(grupo.id, grupo.nombre, grupo.semestre, persona(grupo.docente), estudiantes);
    }

    public Contratos.SesionVista sesion(SesionCaso s) {
        return sesion(s, null);
    }

    public Contratos.SesionVista sesion(SesionCaso s, Participacion participacion) {
        Integer progreso = null;
        if (participacion != null) {
            int total = s.caso.escenarios.stream().mapToInt(e -> e.preguntas.size()).sum();
            progreso = total == 0 ? 0 : (int) Math.round(participacion.respuestas.size() * 100.0 / total);
        }
        return new Contratos.SesionVista(s.id, s.codigo, s.estado().name(),
                s.inicio == null ? null : s.inicio.toString(),
                s.fin == null ? null : s.fin.toString(),
                s.tiempoMaxMin, s.toleranciaMin, s.lugar, s.mensaje,
                s.caso.id, s.caso.nombre, s.grupo.id, s.grupo.nombre, s.grupo.semestre,
                s.caso.desarrolloSituacional, s.caso.contexto, false, "",
                participacion == null ? null : participacion.id,
                participacion == null ? null : participacion.estadoNombre(),
                participacion == null ? null : participacion.nota(),
                progreso);
    }
}
