(() => {
  const sim = SimulacionPsicoSocial;
  const anim = PsicoAnimations;
  const cfg = window.PSICOSIM_INIT || {};

  const views = {
    welcome: document.getElementById('welcome-view'),
    scene: document.getElementById('scene-view'),
    question: document.getElementById('question-view'),
    feedback: document.getElementById('feedback-view'),
  };

  const els = {
    stepList: document.getElementById('step-list'),
    scoreDisplay: document.getElementById('score-display'),
    scoreBar: document.getElementById('score-bar'),
    sceneIcon: document.getElementById('scene-icon'),
    sceneBadge: document.getElementById('scene-badge'),
    sceneTitle: document.getElementById('scene-title'),
    sceneContext: document.getElementById('scene-context'),
    sceneNarrative: document.getElementById('scene-narrative'),
    questionStep: document.getElementById('question-step'),
    questionPrompt: document.getElementById('question-prompt'),
    optionsForm: document.getElementById('options-form'),
    feedbackIcon: document.getElementById('feedback-icon'),
    feedbackTitle: document.getElementById('feedback-title'),
    feedbackText: document.getElementById('feedback-text'),
    feedbackMeta: document.getElementById('feedback-meta'),
    feedbackPoints: document.getElementById('feedback-points'),
    btnSubmit: document.getElementById('btn-submit'),
    btnStart: document.getElementById('btn-start'),
    btnContinueScene: document.getElementById('btn-continue-scene'),
    btnNext: document.getElementById('btn-next'),
  };

  let selectedOption = null;
  let currentVisible = views.welcome;
  let pendingSceneTransition = null;

  function feedbackTipo(opt) {
    if (opt.correcta) return 'Por competencia';
    if (opt.puntos > 0) return 'Parcial / normativa';
    return 'Por error';
  }

  async function persistProgress(extra = {}) {
    if (!cfg.participationId) return;
    await fetch('/api/progreso', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        participation_id: cfg.participationId,
        scene_index: sim.state.sceneIndex,
        question_index: sim.state.questionIndex,
        score: sim.state.score,
        phase: sim.state.phase,
        ...extra,
      }),
    });
  }

  function switchView(nextView, direction = 'forward') {
    anim.transitionView(currentVisible, nextView, direction);
    currentVisible = nextView;
  }

  function updateProgress() {
    anim.animateScoreBar(els.scoreBar, els.scoreDisplay, sim.state.score, sim.MAX_SCORE);
    anim.animateSteps(els.stepList, sim.getSteps());
  }

  async function renderScene() {
    const esc = sim.getCurrentEscenario();
    switchView(views.scene, 'forward');
    if (els.sceneIcon) els.sceneIcon.textContent = esc.icono;
    els.sceneBadge.textContent = esc.subtitulo;
    els.sceneTitle.textContent = esc.titulo;
    els.sceneContext.textContent = 'Contexto clínico';
    await anim.typewriter(els.sceneNarrative, esc.narrativa, 12);
    updateProgress();
    persistProgress({ phase: 'scene' });
  }

  function renderQuestion() {
    switchView(views.question, 'forward');
    const esc = sim.getCurrentEscenario();
    const q = sim.getCurrentQuestion();
    els.questionStep.textContent = `${esc.titulo} · Pregunta ${sim.state.questionIndex + 1}/${esc.preguntas.length}`;
    els.questionPrompt.textContent = q.prompt;
    els.optionsForm.innerHTML = '';
    selectedOption = null;
    els.btnSubmit.disabled = true;
    q.opciones.forEach((opt) => {
      const label = document.createElement('label');
      label.className = 'option-card';
      label.innerHTML = `<input type="radio" name="answer" value="${opt.id}"/><span class="option-letter">${opt.id}</span><span class="option-text">${opt.texto}</span>`;
      label.querySelector('input').addEventListener('change', () => {
        selectedOption = opt.id;
        els.btnSubmit.disabled = false;
        document.querySelectorAll('.option-card').forEach((c) => c.classList.remove('selected'));
        label.classList.add('selected');
      });
      els.optionsForm.appendChild(label);
    });
    anim.staggerChildren(els.optionsForm);
    updateProgress();
  }

  function renderFeedback(opt) {
    switchView(views.feedback, 'forward');
    const correct = opt.correcta;
    els.feedbackIcon.className = `feedback-icon ${correct ? 'is-ok' : 'is-warn'}`;
    els.feedbackIcon.innerHTML = `<span class="material-symbols-outlined">${correct ? 'check_circle' : 'info'}</span>`;
    els.feedbackTitle.textContent = correct ? 'Retroalimentación inmediata' : 'Retroalimentación formativa';
    els.feedbackText.textContent = opt.feedback;
    if (els.feedbackMeta) els.feedbackMeta.textContent = `Tipo: ${feedbackTipo(opt)}`;
    els.feedbackPoints.textContent = `+${opt.puntos} pts · Total ${sim.state.score}/${sim.MAX_SCORE}`;
    anim.popFeedback(els.feedbackIcon, correct);
    if (!correct) anim.shakeWrong(views.feedback);
    updateProgress();
    persistProgress({
      phase: 'feedback',
      option_id: opt.id,
      puntos: opt.puntos,
      correcta: opt.correcta,
      feedback_tipo: feedbackTipo(opt),
    });
  }

  function restoreFromServer() {
    if (!cfg.participationId) return;
    sim.state.sceneIndex = cfg.sceneIndex || 0;
    sim.state.questionIndex = cfg.questionIndex || 0;
    sim.state.score = cfg.score || 0;
    if (cfg.resume || cfg.phase === 'question') {
      sim.state.phase = 'question';
      renderQuestion();
    } else if (cfg.sceneIndex > 0 || cfg.questionIndex > 0) {
      sim.state.phase = 'question';
      renderQuestion();
    }
  }

  els.btnStart.addEventListener('click', () => {
    anim.resetScoreDisplay();
    sim.start();
    renderScene();
  });

  els.btnContinueScene.addEventListener('click', () => {
    sim.showQuestion();
    renderQuestion();
  });

  els.btnSubmit.addEventListener('click', () => {
    if (!selectedOption) return;
    const opt = sim.submitAnswer(selectedOption);
    if (opt) renderFeedback(opt);
  });

  els.btnNext.addEventListener('click', () => {
    const prevScene = sim.state.sceneIndex;
    const prevQ = sim.state.questionIndex;
    const next = sim.advance();

    if (next.type === 'scene' && prevScene === 0 && prevQ === 2 && !cfg.resume) {
      window.location.href = window.PSICOSIM_BITACORA_URL;
      return;
    }
    if (next.type === 'scene') renderScene();
    else if (next.type === 'question') renderQuestion();
    else if (next.type === 'done') {
      const base = window.PSICOSIM_RESULTS_URL || '/estudiante/resultados';
      const params = new URLSearchParams({
        puntaje: next.score,
        total: next.max,
        caso: 'Caso 1: Violencia familiar',
        participation_id: cfg.participationId || '',
        case_id: cfg.caseId || '',
      });
      window.location.href = `${base}?${params}`;
    }
  });

  if (cfg.participationId && (cfg.resume || cfg.sceneIndex > 0 || cfg.questionIndex > 0)) {
    sim.state.score = cfg.score || 0;
    restoreFromServer();
  } else {
    anim.animateSteps(els.stepList, sim.getSteps());
  }
})();
