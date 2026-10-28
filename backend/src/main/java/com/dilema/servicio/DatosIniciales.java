package com.dilema.servicio;

import com.dilema.modelo.*;
import com.dilema.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DatosIniciales implements CommandLineRunner {
    private final UsuarioRepo usuarios;
    private final GrupoRepo grupos;
    private final CasoRepo casos;
    private final SesionRepo sesiones;
    private final SesionEstudianteRepo autorizados;
    private final EscenarioPersonajeRepo vinculos;
    private final FabricaCuentas fabrica;
    private final PasswordEncoder cifrado;

    public DatosIniciales(UsuarioRepo usuarios, GrupoRepo grupos, CasoRepo casos, SesionRepo sesiones,
                          SesionEstudianteRepo autorizados, EscenarioPersonajeRepo vinculos,
                          FabricaCuentas fabrica, PasswordEncoder cifrado) {
        this.usuarios = usuarios;
        this.grupos = grupos;
        this.casos = casos;
        this.sesiones = sesiones;
        this.autorizados = autorizados;
        this.vinculos = vinculos;
        this.fabrica = fabrica;
        this.cifrado = cifrado;
    }

    @Override
    @Transactional
    public void run(String... args) {
        fabrica.asegurarCatalogo();
        if (usuarios.count() > 0) {
            usuarios.findAll().forEach(fabrica::otorgarSiVacio);
            return;
        }

        Usuario admin = cuenta("Administración DILEMA", "admin@psicosim.edu", "admin123", Catalogo.Rol.ADMIN, cifrado);
        Usuario docente = cuenta("Docente de práctica", "docente@psicosim.edu", "docente123", Catalogo.Rol.PROFESOR, cifrado);
        Usuario estudiante = new Usuario();
        estudiante.nombre = "Estudiante de práctica";
        estudiante.email = "estudiante@psicosim.edu";
        estudiante.rol = Catalogo.Rol.ESTUDIANTE;
        estudiante.activo = true;
        usuarios.save(admin);
        usuarios.save(docente);
        usuarios.save(estudiante);
        fabrica.otorgarSiVacio(admin);
        fabrica.otorgarSiVacio(docente);

        Grupo grupo = new Grupo();
        grupo.nombre = "Psicología Social";
        grupo.semestre = "2026-I";
        grupo.docente = docente;
        grupo.estudiantes.add(estudiante);
        grupos.save(grupo);

        Caso caso = new Caso();
        caso.nombre = "Caso de estudio inicial";
        caso.desarrolloSituacional = "Un grupo comunitario debe acordar cómo conversar un conflicto entre vecinos. Este caso es un modelo inicial y se reemplazará más adelante.";
        caso.contexto = "La práctica ocurre en un espacio académico. El estudiante elige un enfoque, escucha al grupo y cierra con acuerdos visibles.";
        caso.tiempoEstimadoMin = 40;
        caso.estado = Catalogo.EstadoCaso.PUBLICADO;
        caso.docente = docente;
        caso.escenarios.add(escenario(caso, "Encuadre", "El grupo acaba de llegar. Todavía no hay un acuerdo sobre cómo se va a conversar.", 0,
                pregunta(caso, "¿Cuál es el primer paso al abrir el espacio?", "Piensa en el permiso antes que en la solución.", "Encuadre ético", "2.0", 0,
                        opcion("Presentarte y pedir permiso para conversar.", true, "El encuadre reconoce a las personas antes de intervenir. La relación se construye cuando el otro autoriza la conversación.", 0),
                        opcion("Dar la instrucción y pasar de inmediato a la decisión.", false, "Saltarse el permiso concentra la voz de quien facilita y deja fuera a quien todavía no habló.", 1))));
        caso.escenarios.add(escenario(caso, "Intervención", "Una persona insiste en decidir ya y el resto guarda silencio.", 1,
                pregunta(caso, "¿Cómo respondes a la presión de cerrar la decisión?", null, "Participación del grupo", "1.5", 0,
                        opcion("Abrir un turno breve para que cada persona hable.", true, "El turno devuelve la palabra a quienes no la habían tomado.", 0),
                        opcion("Aceptar la propuesta de quien insiste, para no alargar la sesión.", false, "Ceder a la prisa convierte el silencio del grupo en un falso acuerdo.", 1))));
        caso.escenarios.add(escenario(caso, "Cierre", "La conversación ya produjo una idea, pero no quedó escrita.", 2,
                pregunta(caso, "¿Qué dejas registrado al cerrar?", null, "Cierre y registro", "1.5", 0,
                        opcion("Los acuerdos, los disensos y el siguiente paso.", true, "El registro permite retomar la conversación sin perder matices.", 0),
                        opcion("Solo la decisión final, en una frase.", false, "Una frase única borra quién estuvo de acuerdo y qué quedó pendiente.", 1))));
        String[] nombres = {"Ana", "Luis", "Marta", "Pedro"};
        for (String nombre : nombres) {
            Personaje personaje = new Personaje();
            personaje.caso = caso;
            personaje.nombre = nombre;
            personaje.descripcion = "Participa en la conversación del caso.";
            caso.personajes.add(personaje);
        }
        casos.save(caso);
        for (Escenario escenario : caso.escenarios) {
            for (Pregunta pregunta : escenario.preguntas) pregunta.casoId = caso.id;
        }
        for (Personaje personaje : caso.personajes) {
            for (Escenario escenario : caso.escenarios) {
                EscenarioPersonaje vinculo = new EscenarioPersonaje();
                vinculo.escenarioId = escenario.id;
                vinculo.personajeId = personaje.id;
                vinculo.casoId = caso.id;
                vinculos.save(vinculo);
            }
        }

        SesionCaso sesion = new SesionCaso();
        sesion.caso = caso;
        sesion.grupo = grupo;
        sesion.codigo = "DILEMA-2026";
        sesion.inicio = LocalDateTime.now().minusHours(1);
        sesion.fin = LocalDateTime.now().plusDays(20);
        sesion.tiempoMaxMin = 30;
        sesion.toleranciaMin = 180;
        sesion.lugar = "Aula 3";
        sesion.mensaje = "Bienvenida a la práctica. El caso de estudio es inicial y luego se va a cambiar.";
        sesion.inicioReal = LocalDateTime.now().minusMinutes(15);
        sesiones.save(sesion);

        SesionEstudiante fila = new SesionEstudiante();
        fila.sesionId = sesion.id;
        fila.estudianteId = estudiante.id;
        fila.grupoId = grupo.id;
        autorizados.save(fila);
    }

    private Usuario cuenta(String nombre, String email, String clave, Catalogo.Rol rol, PasswordEncoder cifrado) {
        Usuario usuario = new Usuario();
        usuario.nombre = nombre;
        usuario.email = email;
        usuario.contrasena = cifrado.encode(clave);
        usuario.rol = rol;
        usuario.activo = true;
        return usuario;
    }

    private Escenario escenario(Caso caso, String titulo, String contexto, int orden, Pregunta pregunta) {
        Escenario escenario = new Escenario();
        escenario.caso = caso;
        escenario.titulo = titulo;
        escenario.contexto = contexto;
        escenario.orden = orden;
        pregunta.escenario = escenario;
        escenario.preguntas.add(pregunta);
        return escenario;
    }

    private Pregunta pregunta(Caso caso, String enunciado, String pista, String criterio, String peso, int orden, Opcion... opciones) {
        Pregunta pregunta = new Pregunta();
        pregunta.casoId = caso.id == null ? 0L : caso.id;
        pregunta.enunciado = enunciado;
        pregunta.pista = pista;
        pregunta.orden = orden;
        CriterioRubrica rubrica = new CriterioRubrica();
        rubrica.pregunta = pregunta;
        rubrica.criterio = criterio;
        rubrica.peso = new BigDecimal(peso);
        pregunta.criterio = rubrica;
        int i = 0;
        for (Opcion opcion : opciones) {
            opcion.orden = i++;
            opcion.pregunta = pregunta;
            pregunta.opciones.add(opcion);
        }
        return pregunta;
    }

    private Opcion opcion(String texto, boolean correcta, String explicacion, int orden) {
        Opcion opcion = new Opcion();
        opcion.descripcion = texto;
        opcion.esCorrecta = correcta;
        opcion.orden = orden;
        Retroalimentacion retro = new Retroalimentacion();
        retro.opcion = opcion;
        retro.explicacion = explicacion;
        opcion.retroalimentacion = retro;
        return opcion;
    }
}
