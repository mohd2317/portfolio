/*
 * Portfolio front end (vanilla JS, no dependencies, CSP-friendly: no inline code).
 *
 * Responsibilities
 *  1. Fetch content from the REST API and render it.
 *  2. Motion: typed hero text, scroll reveal, counters, skill bars, progress bar, carousel.
 *  3. UX: theme toggle, mobile menu, active nav link, project filters.
 *  4. Contact form: client validation + POST with CSRF header.
 *
 * Security: API data is only ever written with textContent / setAttribute (never innerHTML),
 * and link URLs are restricted to http(s) via safeUrl().
 */
(() => {
  'use strict';

  const $ = (sel, root = document) => root.querySelector(sel);
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  /** Tiny DOM builder: el('div', {class:'x'}, child, 'text'). */
  function el(tag, attrs = {}, ...children) {
    const node = document.createElement(tag);
    for (const [k, v] of Object.entries(attrs)) {
      if (v === null || v === undefined || v === false) continue;
      if (k === 'text') node.textContent = v;
      else node.setAttribute(k, v === true ? '' : v);
    }
    for (const c of children) node.append(c);
    return node;
  }

  /** Only allow http(s) links so stored data can never become a javascript: URL. */
  function safeUrl(value) {
    try {
      const u = new URL(value, window.location.origin);
      return (u.protocol === 'https:' || u.protocol === 'http:') ? u.href : null;
    } catch { return null; }
  }

  function getCookie(name) {
    const m = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
    return m ? decodeURIComponent(m[1]) : null;
  }

  async function getJson(url) {
    const res = await fetch(url, { headers: { Accept: 'application/json' }, credentials: 'same-origin' });
    if (!res.ok) throw new Error('Request failed: ' + res.status);
    return res.json();
  }

  /* ---------------------------------------------------------------- Reveal on scroll */
  let revealObserver = null;
  function setupReveal() {
    if (!('IntersectionObserver' in window) || reduceMotion) return;
    revealObserver = new IntersectionObserver((entries) => {
      for (const e of entries) {
        if (!e.isIntersecting) continue;
        e.target.classList.add('visible');
        revealObserver.unobserve(e.target);
        e.target.dispatchEvent(new CustomEvent('revealed'));
      }
    }, { threshold: 0.15, rootMargin: '0px 0px -40px 0px' });
    document.querySelectorAll('.reveal').forEach((n) => revealObserver.observe(n));
  }

  /** Register elements added after page load (rendered from API data). */
  function observeReveal(node, delayIndex = 0) {
    node.classList.add('reveal');
    node.style.setProperty('--d', (delayIndex * 0.08).toFixed(2) + 's');
    if (revealObserver) revealObserver.observe(node);
    else node.classList.add('visible'); // reduced motion / unsupported: show immediately
  }

  /* ---------------------------------------------------------------- Number counters */
  function animateCount(node, target) {
    if (reduceMotion || target === 0) { node.textContent = String(target); return; }
    const duration = 1400;
    const start = performance.now();
    const tick = (now) => {
      const p = Math.min((now - start) / duration, 1);
      const eased = 1 - Math.pow(1 - p, 3);
      node.textContent = String(Math.round(target * eased));
      if (p < 1) requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
  }

  /* ---------------------------------------------------------------- Hero typing effect */
  function typeText(node, text) {
    // Screen readers get the full text immediately; the typed span is decorative.
    node.setAttribute('aria-hidden', 'true');
    node.removeAttribute('aria-label');
    node.after(el('span', { class: 'sr-only', text }));
    if (reduceMotion) { node.textContent = text; return; }
    node.textContent = '';
    let i = 0;
    const timer = setInterval(() => {
      node.textContent = text.slice(0, ++i);
      if (i >= text.length) clearInterval(timer);
    }, 55);
  }

  /* ---------------------------------------------------------------- Renderers */
  function renderProfile(p) {
    document.title = p.fullName + ' | ' + p.title;
    $('#brandName').textContent = p.fullName;
    $('#footerName').textContent = p.fullName;
    $('#heroName').textContent = p.fullName;
    $('#heroTagline').textContent = p.tagline;
    typeText($('#heroRole'), p.title);

    const bio = $('#aboutBio');
    bio.replaceChildren(...p.bio.map((t) => el('p', { text: t })));

    const facts = $('#aboutFacts');
    if (p.location) facts.append(el('li', { text: p.location }));
    facts.append(el('li', { text: p.title }));

    // Counters start when the stats block scrolls into view.
    const stats = $('#stats');
    const run = () => {
      animateCount($('#statYears'), p.yearsExperience);
      animateCount($('#statProjects'), p.projectsCompleted);
      animateCount($('#statClients'), p.happyClients);
    };
    if (revealObserver) stats.addEventListener('revealed', run, { once: true });
    else run();

    // Contact details
    const list = $('#contactList');
    const mail = el('a', { href: 'mailto:' + p.email, text: p.email });
    list.append(el('li', {}, el('span', { class: 'k', text: 'Email' }), mail));
    if (p.location) list.append(el('li', {}, el('span', { class: 'k', text: 'Location' }), el('span', { text: p.location })));

    // Footer social links
    const social = $('#footerSocial');
    for (const [label, url] of [['GitHub', p.githubUrl], ['LinkedIn', p.linkedinUrl]]) {
      const href = url && safeUrl(url);
      if (href) social.append(el('li', {}, el('a', { href, rel: 'noopener noreferrer', target: '_blank', text: label })));
    }
  }

  function renderSkills(skills) {
    const grid = $('#skillsGrid');
    const byCat = new Map();
    skills.forEach((s) => byCat.set(s.category, [...(byCat.get(s.category) || []), s]));

    let idx = 0;
    for (const [cat, items] of byCat) {
      const card = el('article', { class: 'card skill-card' }, el('h3', { text: cat }));
      for (const s of items) {
        const fill = el('div', { class: 'bar-fill' });
        fill.dataset.level = String(s.level);
        card.append(el('div', { class: 'skill' },
          el('div', { class: 'skill-top' }, el('span', { text: s.name }), el('span', { text: s.level + '%' })),
          el('div', { class: 'bar', role: 'progressbar', 'aria-label': s.name, 'aria-valuemin': '0', 'aria-valuemax': '100', 'aria-valuenow': String(s.level) }, fill)));
      }
      observeReveal(card, idx++);
      // Fill the bars when the card becomes visible.
      const fillBars = () => card.querySelectorAll('.bar-fill').forEach((b) => { b.style.width = b.dataset.level + '%'; });
      if (revealObserver) card.addEventListener('revealed', fillBars, { once: true });
      else fillBars();
      grid.append(card);
    }
  }

  function renderProjects(projects) {
    const grid = $('#projectsGrid');
    const filters = $('#projectFilters');
    const techs = [...new Set(projects.flatMap((p) => p.techStack))].sort();

    projects.forEach((p, i) => {
      const links = el('div', { class: 'links' });
      const repo = p.repoUrl && safeUrl(p.repoUrl);
      const live = p.liveUrl && safeUrl(p.liveUrl);
      if (repo) links.append(el('a', { href: repo, target: '_blank', rel: 'noopener noreferrer', text: 'Source code' }));
      if (live) links.append(el('a', { href: live, target: '_blank', rel: 'noopener noreferrer', text: 'Live demo' }));

      const card = el('article', { class: 'card project' },
        p.featured ? el('span', { class: 'badge', text: 'Featured' }) : null,
        el('h3', { text: p.title }),
        el('p', { text: p.summary }),
        el('ul', { class: 'tags', 'aria-label': 'Technologies' }, ...p.techStack.map((t) => el('li', { text: t }))),
        links);
      card.dataset.tech = p.techStack.join('|');
      observeReveal(card, i % 3);
      grid.append(card);
    });

    // Filter chips (buttons with aria-pressed so state is announced)
    const chips = ['All', ...techs.slice(0, 7)].map((label) => {
      const b = el('button', { class: 'chip', type: 'button', 'aria-pressed': label === 'All' ? 'true' : 'false', text: label });
      b.addEventListener('click', () => {
        chips.forEach((c) => c.setAttribute('aria-pressed', String(c === b)));
        grid.querySelectorAll('.project').forEach((card) => {
          const show = label === 'All' || card.dataset.tech.split('|').includes(label);
          card.classList.toggle('hide', !show);
          if (show) { card.classList.remove('pop'); void card.offsetWidth; card.classList.add('pop'); }
        });
      });
      return b;
    });
    filters.append(...chips);
  }

  function renderTestimonials(items) {
    const track = $('#carouselTrack');
    const dots = $('#dots');
    const carousel = $('#carousel');
    let current = 0;
    let timer = null;

    const slides = items.map((t, i) => {
      const slide = el('figure', { class: 'card quote', role: 'group', 'aria-roledescription': 'slide', 'aria-label': (i + 1) + ' of ' + items.length },
        el('blockquote', { text: t.quote }),
        el('figcaption', {}, el('cite', { text: t.author }), el('span', { class: 'role', text: t.role })));
      track.append(slide);
      return slide;
    });
    const dotEls = items.map((_, i) => {
      const d = el('button', { class: 'dot', type: 'button', role: 'tab', 'aria-label': 'Show testimonial ' + (i + 1) });
      d.addEventListener('click', () => { show(i); restart(); });
      dots.append(d);
      return d;
    });

    function show(i) {
      current = (i + slides.length) % slides.length;
      slides.forEach((s, n) => { s.classList.toggle('active', n === current); s.setAttribute('aria-hidden', String(n !== current)); });
      dotEls.forEach((d, n) => d.setAttribute('aria-selected', String(n === current)));
    }
    const start = () => { if (!reduceMotion && slides.length > 1) timer = setInterval(() => show(current + 1), 6500); };
    const stop = () => { clearInterval(timer); timer = null; };
    const restart = () => { stop(); start(); };

    $('#prevBtn').addEventListener('click', () => { show(current - 1); restart(); });
    $('#nextBtn').addEventListener('click', () => { show(current + 1); restart(); });
    // Pause auto-rotation on hover/focus so people have time to read (WCAG 2.2.2).
    carousel.addEventListener('mouseenter', stop);
    carousel.addEventListener('mouseleave', start);
    carousel.addEventListener('focusin', stop);
    carousel.addEventListener('focusout', start);
    carousel.addEventListener('keydown', (e) => {
      if (e.key === 'ArrowLeft') { show(current - 1); restart(); }
      if (e.key === 'ArrowRight') { show(current + 1); restart(); }
    });

    show(0);
    start();
  }

  /* ---------------------------------------------------------------- Data loading */
  async function loadContent() {
    const results = await Promise.allSettled([
      getJson('/api/profile'), getJson('/api/skills'), getJson('/api/projects'), getJson('/api/testimonials'),
    ]);
    const [profile, skills, projects, testimonials] = results;
    if (profile.status === 'fulfilled') renderProfile(profile.value);
    if (skills.status === 'fulfilled') renderSkills(skills.value);
    if (projects.status === 'fulfilled') renderProjects(projects.value);
    if (testimonials.status === 'fulfilled') renderTestimonials(testimonials.value);
    const failed = results.filter((r) => r.status === 'rejected').length;
    if (failed) {
      // Single, polite message; details stay in the console only.
      const note = el('p', { class: 'form-status fail', role: 'status', text: 'Some content could not be loaded. Please refresh the page.' });
      $('#main').prepend(note);
    }
  }

  /* ---------------------------------------------------------------- Header, nav, theme */
  function setupChrome() {
    const header = $('#header');
    const progress = $('#progress');
    const menu = $('#navMenu');
    const toggle = $('#navToggle');
    const links = [...document.querySelectorAll('.nav-link')];
    const sections = links.map((l) => $(l.getAttribute('href')));

    const onScroll = () => {
      const max = document.documentElement.scrollHeight - window.innerHeight;
      progress.style.transform = 'scaleX(' + (max > 0 ? window.scrollY / max : 0) + ')';
      header.classList.toggle('scrolled', window.scrollY > 10);
    };
    let ticking = false;
    window.addEventListener('scroll', () => {
      if (!ticking) { ticking = true; requestAnimationFrame(() => { onScroll(); ticking = false; }); }
    }, { passive: true });
    onScroll();

    // Highlight the nav link of the section in view
    if ('IntersectionObserver' in window) {
      const spy = new IntersectionObserver((entries) => {
        for (const e of entries) {
          if (!e.isIntersecting) continue;
          links.forEach((l) => {
            const active = l.getAttribute('href') === '#' + e.target.id;
            l.classList.toggle('active', active);
            if (active) l.setAttribute('aria-current', 'true'); else l.removeAttribute('aria-current');
          });
        }
      }, { rootMargin: '-45% 0px -50% 0px' });
      sections.forEach((s) => s && spy.observe(s));
    }

    // Mobile menu
    const setMenu = (open) => { menu.classList.toggle('open', open); toggle.setAttribute('aria-expanded', String(open)); };
    toggle.addEventListener('click', () => setMenu(!menu.classList.contains('open')));
    links.forEach((l) => l.addEventListener('click', () => setMenu(false)));
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && menu.classList.contains('open')) { setMenu(false); toggle.focus(); } });

    // Theme
    const themeBtn = $('#themeToggle');
    const syncLabel = () => {
      const dark = document.documentElement.getAttribute('data-theme') !== 'light';
      themeBtn.setAttribute('aria-label', dark ? 'Switch to light theme' : 'Switch to dark theme');
    };
    themeBtn.addEventListener('click', () => {
      const next = document.documentElement.getAttribute('data-theme') === 'light' ? 'dark' : 'light';
      document.documentElement.setAttribute('data-theme', next);
      try { localStorage.setItem('theme', next); } catch { /* storage may be blocked */ }
      syncLabel();
    });
    syncLabel();

    $('#year').textContent = String(new Date().getFullYear());
  }

  /* ---------------------------------------------------------------- Contact form */
  function setupForm() {
    const form = $('#contactForm');
    const status = $('#formStatus');
    const btn = $('#submitBtn');
    const fields = ['name', 'email', 'subject', 'message'];

    const setError = (id, msg) => {
      const input = $('#' + id);
      $('#' + id + '-err').textContent = msg || '';
      input.closest('.field').classList.toggle('invalid', Boolean(msg));
      if (msg) input.setAttribute('aria-invalid', 'true'); else input.removeAttribute('aria-invalid');
    };

    function validate() {
      let firstBad = null;
      const rules = {
        name: (v) => (!v ? 'Please enter your name.' : ''),
        email: (v) => (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) ? 'Please enter a valid email address.' : ''),
        subject: (v) => (!v ? 'Please enter a subject.' : ''),
        message: (v) => (v.length < 10 ? 'Your message should be at least 10 characters.' : ''),
      };
      for (const id of fields) {
        const msg = rules[id]($('#' + id).value.trim());
        setError(id, msg);
        if (msg && !firstBad) firstBad = id;
      }
      if (firstBad) $('#' + firstBad).focus();
      return !firstBad;
    }

    fields.forEach((id) => $('#' + id).addEventListener('input', () => setError(id, '')));

    form.addEventListener('submit', async (ev) => {
      ev.preventDefault();
      status.className = 'form-status';
      status.textContent = '';
      if (!validate()) return;

      btn.disabled = true;
      btn.textContent = 'Sending…';
      try {
        const headers = { 'Content-Type': 'application/json', Accept: 'application/json' };
        const xsrf = getCookie('XSRF-TOKEN');
        if (xsrf) headers['X-XSRF-TOKEN'] = xsrf;
        const res = await fetch('/api/contact', {
          method: 'POST', headers, credentials: 'same-origin',
          body: JSON.stringify({
            name: $('#name').value.trim(), email: $('#email').value.trim(),
            subject: $('#subject').value.trim(), message: $('#message').value.trim(),
            website: $('#website').value,
          }),
        });
        if (res.status === 201) {
          form.reset();
          status.className = 'form-status ok';
          status.textContent = 'Thank you! Your message has been sent.';
        } else if (res.status === 400) {
          const body = await res.json().catch(() => ({}));
          for (const [field, msg] of Object.entries(body.errors || {})) if (fields.includes(field)) setError(field, msg);
          status.className = 'form-status fail';
          status.textContent = 'Please correct the highlighted fields.';
        } else if (res.status === 429) {
          status.className = 'form-status fail';
          status.textContent = 'Too many messages. Please try again later.';
        } else {
          throw new Error('status ' + res.status);
        }
      } catch {
        status.className = 'form-status fail';
        status.textContent = 'Sorry, something went wrong. Please try again.';
      } finally {
        btn.disabled = false;
        btn.textContent = 'Send message';
      }
    });
  }

  /* ---------------------------------------------------------------- Boot */
  function init() {
    setupReveal();
    setupChrome();
    setupForm();
    loadContent();
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})();
