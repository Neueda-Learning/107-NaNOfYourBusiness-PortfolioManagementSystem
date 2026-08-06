/**
 * animations.js — Premium Motion Layer
 * Handles: theme toggle, custom cursor (lerp), preloader, stagger reveals,
 *          header scroll-shadow, card 3D tilt
 */

// ── 1. Theme Toggle (runs immediately — avoids FOUC) ────────────
const themeToggle = document.getElementById('theme-toggle');
themeToggle?.addEventListener('click', () => {
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
  const next   = isDark ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  localStorage.setItem('pm-theme', next);
  window.dispatchEvent(new CustomEvent('themechange', { detail: { theme: next } }));
});

// ── 2. Custom Cursor (desktop only) ─────────────────────────────
const cursorDot     = document.getElementById('cursor-dot');
const cursorOutline = document.getElementById('cursor-outline');
const ENABLE_CUSTOM_CURSOR = false;
const hasFinePointer = window.matchMedia('(hover: hover) and (pointer: fine)').matches;

if (ENABLE_CUSTOM_CURSOR && cursorDot && cursorOutline && hasFinePointer) {
  let mx = window.innerWidth  / 2;
  let my = window.innerHeight / 2;
  let ox = mx, oy = my;

  // Snap dot immediately to mouse
  window.addEventListener('mousemove', e => {
    mx = e.clientX;
    my = e.clientY;
    cursorDot.style.transform = `translate(${mx - 2.5}px, ${my - 2.5}px)`;
  }, { passive: true });

  // Lerp the outline ring for a trailing effect
  (function lerpOutline() {
    ox += (mx - ox) * 0.11;
    oy += (my - oy) * 0.11;
    cursorOutline.style.transform = `translate(${ox - 15}px, ${oy - 15}px)`;
    requestAnimationFrame(lerpOutline);
  })();

  // Expand on interactive elements
  const hoverTargets = 'a, button, [role="tab"], .card, .chart-card, .btn-retry';
  document.querySelectorAll(hoverTargets).forEach(el => {
    el.addEventListener('mouseenter', () => {
      cursorDot.classList.add('is-hovering');
      cursorOutline.classList.add('is-hovering');
    });
    el.addEventListener('mouseleave', () => {
      cursorDot.classList.remove('is-hovering');
      cursorOutline.classList.remove('is-hovering');
    });
  });
}

// ── 3. Preloader ─────────────────────────────────────────────────
const preloader = document.getElementById('preloader');
if (preloader) {
  // Total letter animation time ≈ 1.25s, then hold briefly
  const HOLD_MS = 1500;
  setTimeout(() => {
    preloader.classList.add('is-done');
    preloader.addEventListener('transitionend', () => {
      preloader.remove();
      // Trigger initial stagger on whichever panel is already active
      document.querySelectorAll('.tab-panel.active').forEach(triggerStagger);
    }, { once: true });
  }, HOLD_MS);
}

// ── 4. Stagger entrance on tab switch ───────────────────────────
function triggerStagger(panel) {
  const children = panel.querySelectorAll('.stagger-child');
  children.forEach(el => el.classList.remove('is-visible'));
  // Double rAF to ensure browser registers the class removal first
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      children.forEach(el => el.classList.add('is-visible'));
    });
  });
}

// Watch for the `active` class being added to any .tab-panel
const panelObserver = new MutationObserver(mutations => {
  mutations.forEach(({ target, oldValue }) => {
    const wasActive = (oldValue || '').split(' ').includes('active');
    if (!wasActive && target.classList.contains('active')) {
      triggerStagger(target);
    }
  });
});

document.querySelectorAll('.tab-panel').forEach(panel => {
  panelObserver.observe(panel, {
    attributes: true,
    attributeFilter: ['class'],
    attributeOldValue: true,
  });
});

// ── 5. Header scroll-shadow ──────────────────────────────────────
const appHeader = document.querySelector('.app-header');
window.addEventListener('scroll', () => {
  appHeader?.classList.toggle('is-scrolled', window.scrollY > 10);
}, { passive: true });

// ── 6. Card 3D tilt micro-interaction (desktop only) ────────────
// Skipped for `.card--static` (cards containing data tables — a tilting/lifting
// card while the user tries to click a row action feels broken, not premium).
if (hasFinePointer) {
  document.querySelectorAll('.card, .chart-card').forEach(card => {
    if (card.classList.contains('card--static')) return;
    card.addEventListener('mousemove', e => {
      const rect   = card.getBoundingClientRect();
      const cx     = rect.left + rect.width  / 2;
      const cy     = rect.top  + rect.height / 2;
      const dx     = (e.clientX - cx) / (rect.width  / 2);
      const dy     = (e.clientY - cy) / (rect.height / 2);
      const tiltX  = dy * -4;   // degrees
      const tiltY  = dx *  4;
      card.style.transform = `perspective(600px) rotateX(${tiltX}deg) rotateY(${tiltY}deg) translateY(-2px)`;
      card.style.willChange = 'transform';
    });
    card.addEventListener('mouseleave', () => {
      card.style.transform = '';
      card.style.willChange = '';
    });
  });
}

