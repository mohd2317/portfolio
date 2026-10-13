/*
 * Admin console: session login, contact message inbox and project CRUD.
 * Authorisation is enforced by the server; this page only hides/shows views.
 * All dynamic text is set via textContent (no innerHTML).
 */
(() => {
  'use strict';
  const $ = (s) => document.querySelector(s);

  function el(tag, attrs = {}, ...children) {
    const n = document.createElement(tag);
    for (const [k, v] of Object.entries(attrs)) {
      if (v === null || v === undefined || v === false) continue;
      if (k === 'text') n.textContent = v; else n.setAttribute(k, v === true ? '' : v);
    }
    n.append(...children);
    return n;
  }

  const cookie = (name) => {
    const m = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
    return m ? decodeURIComponent(m[1]) : null;
  };

  /** fetch wrapper adding JSON headers + CSRF token; returns the Response. */
  async function api(url, { method = 'GET', body } = {}) {
    const headers = { Accept: 'application/json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    const xsrf = cookie('XSRF-TOKEN');
    if (xsrf && method !== 'GET') headers['X-XSRF-TOKEN'] = xsrf;
    return fetch(url, { method, headers, credentials: 'same-origin', body: body === undefined ? undefined : JSON.stringify(body) });
  }

  const status = (msg, ok = true) => {
    const s = $('#status');
    s.textContent = msg;
    s.className = 'form-status ' + (msg ? (ok ? 'ok' : 'fail') : '');
  };

  function show(loggedIn) {
    $('#loginView').hidden = loggedIn;
    $('#dashboard').hidden = !loggedIn;
    $('#logoutBtn').hidden = !loggedIn;
  }

  /* ---------- Messages ---------- */
  async function loadMessages() {
    const res = await api('/api/admin/messages');
    if (res.status === 401 || res.status === 403) return show(false);
    const rows = await res.json();
    const body = $('#messagesBody');
    body.replaceChildren();
    if (!rows.length) body.append(el('tr', {}, el('td', { colspan: '5', text: 'No messages yet.' })));
    for (const m of rows) {
      const actions = el('div', { class: 'row-actions' });
      if (!m.read) {
        const b = el('button', { class: 'btn btn-ghost btn-sm', type: 'button', text: 'Mark read' });
        b.addEventListener('click', async () => { await api('/api/admin/messages/' + m.id + '/read', { method: 'PATCH' }); loadMessages(); });
        actions.append(b);
      }
      const del = el('button', { class: 'btn btn-danger btn-sm', type: 'button', text: 'Delete' });
      del.addEventListener('click', async () => {
        if (!confirm('Delete this message?')) return;
        await api('/api/admin/messages/' + m.id, { method: 'DELETE' });
        loadMessages();
      });
      actions.append(del);
      body.append(el('tr', { class: m.read ? '' : 'unread' },
        el('td', {}, el('div', { text: m.name }), el('a', { href: 'mailto:' + m.email, text: m.email })),
        el('td', { text: m.subject }),
        el('td', { text: m.message }),
        el('td', { text: new Date(m.createdAt).toLocaleString() }),
        el('td', {}, actions)));
    }
  }

  /* ---------- Projects ---------- */
  let projectCache = [];
  async function loadProjects() {
    const res = await fetch('/api/projects', { headers: { Accept: 'application/json' } });
    projectCache = await res.json();
    const body = $('#projectsBody');
    body.replaceChildren();
    for (const p of projectCache) {
      const edit = el('button', { class: 'btn btn-ghost btn-sm', type: 'button', text: 'Edit' });
      edit.setAttribute('aria-label', 'Edit ' + p.title);
      edit.addEventListener('click', () => startEdit(p));
      const del = el('button', { class: 'btn btn-danger btn-sm', type: 'button', text: 'Delete' });
      del.setAttribute('aria-label', 'Delete ' + p.title);
      del.addEventListener('click', async () => {
        if (!confirm('Delete project "' + p.title + '"?')) return;
        const r = await api('/api/admin/projects/' + p.id, { method: 'DELETE' });
        status(r.ok ? 'Project deleted.' : 'Could not delete project.', r.ok);
        loadProjects();
      });
      body.append(el('tr', {},
        el('td', { text: String(p.displayOrder) }),
        el('td', { text: p.title + (p.featured ? ' (featured)' : '') }),
        el('td', { text: p.techStack.join(', ') }),
        el('td', {}, el('div', { class: 'row-actions' }, edit, del))));
    }
  }

  function startEdit(p) {
    $('#projectId').value = p.id;
    $('#p-title').value = p.title;
    $('#p-tech').value = p.techStack.join(', ');
    $('#p-summary').value = p.summary;
    $('#p-repo').value = p.repoUrl || '';
    $('#p-live').value = p.liveUrl || '';
    $('#p-order').value = p.displayOrder;
    $('#p-featured').checked = p.featured;
    $('#formTitle').textContent = 'Edit project';
    $('#cancelEdit').hidden = false;
    $('#p-title').focus();
  }

  function resetForm() {
    $('#projectForm').reset();
    $('#projectId').value = '';
    $('#p-order').value = 100;
    $('#formTitle').textContent = 'Add project';
    $('#cancelEdit').hidden = true;
  }

  $('#cancelEdit').addEventListener('click', resetForm);

  $('#projectForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = $('#projectId').value;
    const payload = {
      title: $('#p-title').value.trim(),
      summary: $('#p-summary').value.trim(),
      techStack: $('#p-tech').value.trim(),
      repoUrl: $('#p-repo').value.trim(),
      liveUrl: $('#p-live').value.trim(),
      featured: $('#p-featured').checked,
      displayOrder: Number($('#p-order').value),
    };
    const res = await api(id ? '/api/admin/projects/' + id : '/api/admin/projects', { method: id ? 'PUT' : 'POST', body: payload });
    if (res.ok) {
      status('Project saved.');
      resetForm();
      loadProjects();
    } else if (res.status === 400) {
      const err = await res.json().catch(() => ({}));
      status('Validation failed: ' + Object.keys(err.errors || {}).join(', '), false);
    } else {
      status('Could not save project.', false);
    }
  });

  /* ---------- Login / logout ---------- */
  $('#loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = new URLSearchParams({ username: $('#username').value, password: $('#password').value });
    const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
    const xsrf = cookie('XSRF-TOKEN');
    if (xsrf) headers['X-XSRF-TOKEN'] = xsrf;
    const res = await fetch('/api/auth/login', { method: 'POST', headers, body: form, credentials: 'same-origin' });
    $('#password').value = '';
    if (res.ok) { status(''); await enter(); }
    else status(res.status === 429 ? 'Too many attempts. Try again later.' : 'Invalid username or password.', false);
  });

  $('#logoutBtn').addEventListener('click', async () => {
    await api('/api/auth/logout', { method: 'POST' });
    show(false);
    status('Signed out.');
  });

  async function enter() {
    show(true);
    await Promise.all([loadMessages(), loadProjects()]);
  }

  async function init() {
    // Make sure the XSRF cookie exists before the first POST.
    const me = await api('/api/auth/me');
    if (me.ok) await enter(); else show(false);
  }
  init();
})();
