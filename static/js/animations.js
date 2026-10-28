/**
 * Animaciones del simulador — transiciones, contadores y efectos visuales
 */
const PsicoAnimations = (() => {
  let displayScore = 0;

  function easeOutCubic(t) {
    return 1 - (1 - t) ** 3;
  }

  function animateValue(from, to, duration, onUpdate, onDone) {
    const start = performance.now();
    function frame(now) {
      const t = Math.min(1, (now - start) / duration);
      onUpdate(from + (to - from) * easeOutCubic(t));
      if (t < 1) requestAnimationFrame(frame);
      else if (onDone) onDone();
    }
    requestAnimationFrame(frame);
  }

  function countUp(el, target, suffix = '') {
    if (!el) return;
    animateValue(0, target, 900, (v) => {
      el.textContent = `${Math.round(v)}${suffix}`;
    });
  }

  function transitionView(hideEl, showEl, direction = 'forward') {
    const outClass = direction === 'forward' ? 'view-exit-left' : 'view-exit-right';
    const inClass = direction === 'forward' ? 'view-enter-right' : 'view-enter-left';

    if (hideEl && !hideEl.hidden) {
      hideEl.classList.add(outClass);
      hideEl.addEventListener('animationend', function handler() {
        hideEl.removeEventListener('animationend', handler);
        hideEl.hidden = true;
        hideEl.classList.remove(outClass);
      }, { once: true });
    }

    if (showEl) {
      showEl.hidden = false;
      showEl.classList.remove('view-enter-right', 'view-enter-left');
      void showEl.offsetWidth;
      showEl.classList.add(inClass);
      showEl.addEventListener('animationend', function handler() {
        showEl.removeEventListener('animationend', handler);
        showEl.classList.remove(inClass);
      }, { once: true });
    }
  }

  function staggerChildren(container, itemSelector = '.option-card') {
    if (!container) return;
    const items = container.querySelectorAll(itemSelector);
    items.forEach((item, i) => {
      item.style.setProperty('--stagger', `${i * 0.07}s`);
      item.classList.add('anim-stagger-in');
    });
  }

  function animateScoreBar(barEl, displayEl, score, max) {
    const pct = (score / max) * 100;
    if (barEl) {
      barEl.classList.add('progress-pulse');
      barEl.style.width = `${pct}%`;
      setTimeout(() => barEl.classList.remove('progress-pulse'), 600);
    }
    animateValue(displayScore, score, 500, (v) => {
      displayScore = v;
      if (displayEl) displayEl.textContent = `${Math.round(v)} / ${max}`;
    });
  }

  function animateSteps(stepListEl, steps) {
    if (!stepListEl) return;
    stepListEl.innerHTML = '';
    steps.forEach((step, i) => {
      const li = document.createElement('li');
      li.className = `step-item step-item--${step.status} anim-step-in`;
      li.style.setProperty('--stagger', `${i * 0.05}s`);
      li.textContent = step.label;
      stepListEl.appendChild(li);
    });
  }

  function typewriter(el, text, speed = 18) {
    if (!el) return Promise.resolve();
    el.textContent = '';
    el.classList.add('is-typing');
    return new Promise((resolve) => {
      let i = 0;
      function tick() {
        if (i <= text.length) {
          el.textContent = text.slice(0, i);
          i += 1;
          setTimeout(tick, speed);
        } else {
          el.classList.remove('is-typing');
          resolve();
        }
      }
      tick();
    });
  }

  function popFeedback(iconEl, correct) {
    if (!iconEl) return;
    iconEl.classList.remove('anim-pop-feedback');
    void iconEl.offsetWidth;
    iconEl.classList.add('anim-pop-feedback');
    if (correct) burstParticles();
  }

  function burstParticles() {
    const canvas = document.getElementById('fx-canvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width;
    canvas.height = rect.height;

    const particles = Array.from({ length: 24 }, () => ({
      x: canvas.width * 0.5,
      y: canvas.height * 0.35,
      vx: (Math.random() - 0.5) * 6,
      vy: (Math.random() - 1.2) * 5,
      life: 1,
      color: Math.random() > 0.5 ? '#6ee7b7' : '#a78bfa',
      size: 3 + Math.random() * 4,
    }));

    let frame = 0;
    function draw() {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      particles.forEach((p) => {
        p.x += p.vx;
        p.y += p.vy;
        p.vy += 0.12;
        p.life -= 0.025;
        if (p.life <= 0) return;
        ctx.globalAlpha = p.life;
        ctx.fillStyle = p.color;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
        ctx.fill();
      });
      frame += 1;
      if (frame < 50) requestAnimationFrame(draw);
      else ctx.clearRect(0, 0, canvas.width, canvas.height);
    }
    draw();
  }

  function shakeWrong(el) {
    if (!el) return;
    el.classList.add('anim-shake');
    el.addEventListener('animationend', () => el.classList.remove('anim-shake'), { once: true });
  }

  function initLanding() {
    document.querySelectorAll('[data-count]').forEach((el) => {
      countUp(el, parseInt(el.dataset.count, 10));
    });
    const bar = document.querySelector('.anim-progress-fill');
    if (bar) {
      setTimeout(() => {
        bar.style.width = `${bar.dataset.target || 35}%`;
      }, 400);
    }
  }

  function initResults() {
    const pctEl = document.getElementById('results-percent');
    const scoreEl = document.getElementById('score-final');
    if (pctEl) countUp(pctEl, parseInt(pctEl.dataset.target, 10), '%');
    if (scoreEl) countUp(scoreEl, parseInt(scoreEl.dataset.target, 10));
  }

  function resetScoreDisplay() {
    displayScore = 0;
  }

  return {
    transitionView,
    staggerChildren,
    animateScoreBar,
    animateSteps,
    typewriter,
    popFeedback,
    shakeWrong,
    initLanding,
    initResults,
    resetScoreDisplay,
  };
})();
