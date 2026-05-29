/**
 * Motor de simulación — Caso 1: Violencia familiar y tentativa de feminicidio
 */
const SimulacionPsicoSocial = (() => {
  const MAX_SCORE = 100;
  const POINTS_PER_QUESTION = Math.floor(MAX_SCORE / 6);

  const state = {
    sceneIndex: 0,
    questionIndex: 0,
    score: 0,
    phase: 'welcome',
    lastAnswer: null,
  };

  const escenarios = [
    {
      id: 'hospital',
      titulo: 'Escenario 1: Atención en Hospital',
      subtitulo: 'Urgencia vital y crisis',
      icono: 'local_hospital',
      narrativa:
        'La sobreviviente está en shock hipovolémico y emocional. La familia (madre y hermanos) llega al hospital en estado de alteración, exigiendo ver a la niña, quien ha fallecido, aunque ellos aún no lo saben con certeza.',
      preguntas: [
        {
          prompt: '¿En qué centrar la intervención inmediata?',
          opciones: [
            { id: 'A', texto: 'Notificar a la madre y la familia sobre la muerte de la niña de inmediato.', correcta: false, puntos: 0, feedback: 'En crisis aguda, comunicar la noticia sin preparación ni contención puede aumentar el riesgo de descompensación y revictimización. Primero se estabiliza emocionalmente (PAP).' },
            { id: 'B', texto: 'Contención emocional a la familia, acompañamiento en el duelo inicial y estabilización de la crisis (Primeros Auxilios Psicológicos — PAP).', correcta: true, puntos: POINTS_PER_QUESTION, feedback: 'Correcto. La prioridad es contener la crisis, garantizar seguridad emocional y aplicar PAP antes de intervenciones que requieran mayor estructura.' },
            { id: 'C', texto: 'Interrogar a la víctima herida para obtener detalles del agresor antes de que entre a cirugía.', correcta: false, puntos: Math.floor(POINTS_PER_QUESTION * 0.25), feedback: 'La víctima requiere atención médica urgente. Obtener información forense no puede anteponerse a la estabilización vital ni vulnerar su integridad.' },
          ],
        },
        {
          prompt: '¿Qué marco normativo y técnico debe seguir?',
          opciones: [
            { id: 'A', texto: 'Resolución 459 de 2012 (Protocolo de atención integral a violencias sexuales y basadas en género).', correcta: false, puntos: Math.floor(POINTS_PER_QUESTION * 0.4), feedback: 'La Resolución 459 es central, pero en violencia de género también aplica la Ley 1257 de 2008 para garantizar derechos y rutas de protección.' },
            { id: 'B', texto: 'Resolución 459 de 2012 y Ley 1257 de 2008.', correcta: true, puntos: POINTS_PER_QUESTION, feedback: 'Correcto. Se articulan el protocolo técnico de atención integral y la ley de protección a mujeres víctimas de violencia.' },
            { id: 'C', texto: 'Resolución 459 de 2012 y Ley 1448 de 2011.', correcta: false, puntos: Math.floor(POINTS_PER_QUESTION * 0.3), feedback: 'La Ley 1448 regula víctimas del conflicto armado. No es el marco principal para este caso de violencia intrafamiliar de género.' },
          ],
        },
        {
          prompt: '¿Qué se debe hacer y qué se debe evitar (actuación técnica y ética)?',
          opciones: [
            { id: 'A', texto: 'Escucha activa sin juicios, intervenir disonancia cognitiva, preguntar antecedentes de la relación, activar ruta por psicología clínica y psiquiatría.', correcta: false, puntos: Math.floor(POINTS_PER_QUESTION * 0.5), feedback: 'Parcial. Faltan PAP a la familia, protocolo EPICEE/SPIKES para la noticia de fallecimiento y trabajo interdisciplinar amplio.' },
            { id: 'B', texto: 'PAP a la familia, escucha activa a la víctima consciente, protocolo EPICEE (SPIKES) para noticia del fallecimiento de la menor.', correcta: false, puntos: Math.floor(POINTS_PER_QUESTION * 0.65), feedback: 'Avance importante, pero aún falta valoración psicosocial familiar, ciclo de violencia e intervención interdisciplinar completa.' },
            { id: 'C', texto: 'PAP, escucha activa, EPICEE/SPIKES, indagar antecedentes del ciclo de violencia y manejo interdisciplinar (psiquiatría, psicoterapia).', correcta: false, puntos: Math.floor(POINTS_PER_QUESTION * 0.8), feedback: 'Muy cercano. La opción más completa incluye además evaluación psicosocial de factores protectores y de riesgo en el duelo.' },
            { id: 'D', texto: 'PAP a la familia, escucha activa a la víctima consciente, EPICEE/SPIKES, evaluación psicosocial de factores protectores y de riesgo en el duelo, e intervención interdisciplinar.', correcta: true, puntos: POINTS_PER_QUESTION, feedback: 'Correcto. Integra contención, comunicación de malas noticias, evaluación del sistema familiar y decisiones clínicas en equipo.' },
          ],
        },
      ],
    },
    {
      id: 'comisaria',
      titulo: 'Escenario 2: Comisaría de Familia',
      subtitulo: 'Restablecimiento de derechos',
      icono: 'balance',
      narrativa:
        'Han pasado 15 días. La mujer ha sido dada de alta, pero presenta secuelas físicas y trauma complejo. Debes definir la medida de protección y el apoyo psicológico a largo plazo.',
      preguntas: [
        {
          prompt: '¿Cuál es la prioridad en la asesoría psicosocial?',
          opciones: [
            { id: 'A', texto: 'Instar a la mujer para que escuche al agresor en pro de la unión familiar (mediación) y el perdón.', correcta: false, puntos: 0, feedback: 'La mediación con el agresor está contraindicada en contextos de violencia severa y riesgo de feminicidio; puede revictimizar.' },
            { id: 'B', texto: 'Valoración del riesgo de feminicidio, activación de medidas de protección y asesoría sobre derechos económicos y de justicia.', correcta: true, puntos: POINTS_PER_QUESTION, feedback: 'Correcto. La seguridad y los derechos de la víctima son la prioridad en comisaría de familia.' },
            { id: 'C', texto: 'Realizar psicoterapia para encontrar patrones de infancia que desencadenan su elección de pareja.', correcta: false, puntos: Math.floor(POINTS_PER_QUESTION * 0.2), feedback: 'La psicoterapia profunda no es la intervención inmediata; primero se garantiza protección y estabilización.' },
          ],
        },
        {
          prompt: '¿Qué marco normativo y técnico debe seguir?',
          opciones: [
            { id: 'A', texto: 'Ley 2126 de 2021, Ley 1098 de 2006, Ley 1257 de 2008.', correcta: true, puntos: POINTS_PER_QUESTION, feedback: 'Correcto. Articula competencias de comisarías (2126), infancia y adolescencia (1098) y violencia contra la mujer (1257).' },
            { id: 'B', texto: 'Ley 1098 de 2006, Ley 1257 de 2008.', correcta: false, puntos: Math.floor(POINTS_PER_QUESTION * 0.55), feedback: 'Incompleto en el contexto de comisaría: la Ley 2126 de 2021 regula funciones y procedimientos actuales de las comisarías.' },
            { id: 'C', texto: 'Ley 1098 de 2006, Ley 1257 de 2008, Ley 1148 de 2011.', correcta: false, puntos: Math.floor(POINTS_PER_QUESTION * 0.25), feedback: 'La referencia a Ley 1148 no corresponde al marco principal; revise Ley 2126 y normativa de infancia y género.' },
          ],
        },
        {
          prompt: '¿Qué se debe hacer y qué se debe evitar (actuación técnica y ética)?',
          opciones: [
            { id: 'A', texto: 'Escucha activa, intervenir disonancia cognitiva, antecedentes de relación, valoración de riesgo de feminicidio, ruta clínica y psiquiatría.', correcta: false, puntos: Math.floor(POINTS_PER_QUESTION * 0.6), feedback: 'Falta valoración inicial integral de dependientes vulnerables y activación formal de rutas de salud mental.' },
            { id: 'B', texto: 'Valoración psicológica inicial de la víctima y dependientes, escucha activa, antecedentes de violencia, derivación a salud mental si es necesario.', correcta: false, puntos: Math.floor(POINTS_PER_QUESTION * 0.75), feedback: 'Buena base, pero debe incluirse explícitamente el nivel de riesgo de vulneración de derechos y valoración de feminicidio.' },
            { id: 'C', texto: 'Valoración psicológica y emocional de la víctima y dependientes, nivel de riesgo de vulneración de derechos, valoración de feminicidio y derivaciones necesarias.', correcta: true, puntos: POINTS_PER_QUESTION, feedback: 'Correcto. Respuesta integral acorde al restablecimiento de derechos y protección.' },
          ],
        },
      ],
    },
  ];

  function getCurrentEscenario() { return escenarios[state.sceneIndex]; }
  function getCurrentQuestion() { return getCurrentEscenario().preguntas[state.questionIndex]; }
  function totalQuestions() { return escenarios.reduce((n, e) => n + e.preguntas.length, 0); }

  function currentGlobalQuestionNumber() {
    let n = 0;
    for (let i = 0; i < state.sceneIndex; i++) n += escenarios[i].preguntas.length;
    return n + state.questionIndex + 1;
  }

  function start() {
    state.sceneIndex = 0;
    state.questionIndex = 0;
    state.score = 0;
    state.phase = 'scene';
    state.lastAnswer = null;
  }

  function showScene() { state.phase = 'scene'; }
  function showQuestion() { state.phase = 'question'; }

  function submitAnswer(optionId) {
    const q = getCurrentQuestion();
    const opt = q.opciones.find((o) => o.id === optionId);
    if (!opt) return null;
    state.lastAnswer = opt;
    state.score = Math.min(MAX_SCORE, state.score + opt.puntos);
    state.phase = 'feedback';
    return opt;
  }

  function advance() {
    const esc = getCurrentEscenario();
    if (state.questionIndex < esc.preguntas.length - 1) {
      state.questionIndex += 1;
      state.phase = 'question';
      return { type: 'question' };
    }
    if (state.sceneIndex < escenarios.length - 1) {
      state.sceneIndex += 1;
      state.questionIndex = 0;
      state.phase = 'scene';
      return { type: 'scene' };
    }
    state.phase = 'done';
    return { type: 'done', score: state.score, max: MAX_SCORE };
  }

  function getSteps() {
    const steps = [];
    let globalIdx = 0;
    const currentIdx =
      escenarios.slice(0, state.sceneIndex).reduce((s, e) => s + e.preguntas.length, 0) +
      state.questionIndex;

    escenarios.forEach((esc) => {
      const short = esc.id === 'hospital' ? 'Hospital' : 'Comisaría';
      esc.preguntas.forEach((_, qi) => {
        let status = 'pending';
        if (state.phase === 'done') status = 'done';
        else if (globalIdx < currentIdx) status = 'done';
        else if (globalIdx === currentIdx && state.phase !== 'welcome') status = 'active';
        steps.push({ label: `${short} · Pregunta ${qi + 1}`, status });
        globalIdx += 1;
      });
    });
    return steps;
  }

  return {
    MAX_SCORE, escenarios, state, totalQuestions, currentGlobalQuestionNumber,
    getCurrentEscenario, getCurrentQuestion, start, showScene, showQuestion,
    submitAnswer, advance, getSteps,
  };
})();
