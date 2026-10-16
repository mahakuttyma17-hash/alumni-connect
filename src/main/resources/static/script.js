/* =====================================================================
   Alumni Connect - script.js
   One shared script for every page. Each page sets <body data-page="...">
   ===================================================================== */
(function () {
  'use strict';

  /* =================================================================
     1. CONSTANTS (keep the two lists identical to register.html)
     ================================================================= */
  const CURRENCY = 'INR';
  const DEPARTMENTS = [
    'Computer Science and Engineering', 'Information Technology',
    'Artificial Intelligence and Data Science', 'Electronics and Communication Engineering',
    'Electrical and Electronics Engineering', 'Mechanical Engineering',
    'Civil Engineering', 'Business Administration', 'Other'
  ];
  const DEGREES = ['B.E. / B.Tech', 'M.E. / M.Tech', 'MBA', 'MCA', 'B.Sc', 'M.Sc', 'Ph.D', 'Other'];
  const MIN_YEAR = 1950;
  const MAX_YEAR = new Date().getFullYear() + 6;
  const TYPE_LABELS = { DONATION: 'Donation', VOLUNTEERING: 'Volunteering', OTHER: 'Other' };
  const TYPE_ICONS = { DONATION: 'i-dollar', VOLUNTEERING: 'i-heart', OTHER: 'i-award' };
  const TYPE_ICON_CLASS = { DONATION: '', VOLUNTEERING: 'is-coral', OTHER: 'is-navy' };
  const STATUS_META = {
    PENDING: { label: 'Pending review', cls: 'badge-coral' },
    VERIFIED: { label: 'Verified', cls: 'badge-teal' },
    REJECTED: { label: 'Rejected', cls: 'badge-danger' }
  };
  const SAFE_NEXT = ['dashboard.html', 'profile.html', 'contributions.html', 'directory.html'];
  const PROFILE_FIELDS = ['fullName', 'email', 'phone', 'department', 'degree', 'graduationYear',
    'company', 'jobTitle', 'city', 'linkedinUrl', 'bio'];

  /* =================================================================
     2. SMALL UTILITIES
     ================================================================= */
  const $ = (sel, root) => (root || document).querySelector(sel);
  const $$ = (sel, root) => Array.from((root || document).querySelectorAll(sel));

function esc(value) {
  return String(value == null ? '' : value).replace(/[&<>"']/g, function (c) {
    return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
  });
}

  function initials(name) {
    const parts = String(name || '').trim().split(/\s+/).filter(Boolean);
    if (!parts.length) return '?';
    return (parts[0][0] + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
  }
  function avatarClass(name) {
    let h = 0;
    for (const ch of String(name || '')) h = (h * 31 + ch.charCodeAt(0)) >>> 0;
    return ['', 'c2', 'c3', 'c4'][h % 4];
  }
  function avatarHtml(name, size) {
    const sizeClass = size ? ' avatar-' + size : '';
    const colour = avatarClass(name) ? ' ' + avatarClass(name) : '';
    return '<span class="avatar' + sizeClass + colour + '" aria-hidden="true">' + esc(initials(name)) + '</span>';
  }
  function icon(name, extra) {
    return '<svg class="icon ' + (extra || '') + '"><use href="icons.svg#' + name + '"/></svg>';
  }
  function parseDate(value) {
    if (!value) return null;
    const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value));
    return m ? new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3])) : new Date(value);
  }
  function formatDate(value) {
    const d = parseDate(value);
    if (!d || isNaN(d)) return '—';
    return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  }
  function formatMoney(n) {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: CURRENCY, maximumFractionDigits: 2 }).format(Number(n) || 0);
  }
  function formatNumber(n) { return Number(n || 0).toLocaleString('en-IN'); }
  function val(v) { return v == null || String(v).trim() === '' ? '—' : esc(v); }
  function nullIfEmpty(v) { v = String(v == null ? '' : v).trim(); return v === '' ? null : v; }
  function safeUrl(url) {
    try {
      const u = new URL(url);
      return u.protocol === 'http:' || u.protocol === 'https:' ? u.href : '';
    } catch (e) { return ''; }
  }
  function todayString() {
    const d = new Date();
    const pad = n => String(n).padStart(2, '0');
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
  }
  function debounce(fn, ms) {
    let t;
    return function () { const args = arguments; clearTimeout(t); t = setTimeout(() => fn.apply(null, args), ms); };
  }
  function currentFile() { return location.pathname.split('/').pop() || 'index.html'; }
  function never() { return new Promise(function () {}); } // halts page init during a redirect

  /* =================================================================
     3. API LAYER (same-origin, session cookie, CSRF)
     ================================================================= */
  class ApiError extends Error {
    constructor(status, message, fieldErrors) {
      super(message);
      this.status = status;
      this.fieldErrors = fieldErrors || {};
    }
  }
  function getCookie(name) {
    const m = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
    return m ? decodeURIComponent(m[1]) : null;
  }
  function refreshCsrf() {
    return fetch('/api/auth/csrf', { credentials: 'same-origin' }).catch(function () {});
  }
  async function ensureCsrf() {
    if (!getCookie('XSRF-TOKEN')) await refreshCsrf();
  }
  function defaultMessage(status) {
    if (status === 401) return 'Please sign in to continue.';
    if (status === 403) return 'You do not have permission to do that.';
    if (status === 404) return 'The requested item was not found.';
    if (status === 409) return 'That record already exists.';
    if (status >= 500) return 'The server had a problem. Please try again.';
    return 'The request could not be completed.';
  }
  function redirectToLogin() {
    location.href = 'login.html?next=' + encodeURIComponent(currentFile());
  }

  async function api(path, opts) {
    opts = opts || {};
    const method = (opts.method || 'GET').toUpperCase();
    const headers = { 'Accept': 'application/json' };
    let body;
    if (opts.form) {
      headers['Content-Type'] = 'application/x-www-form-urlencoded';
      body = new URLSearchParams(opts.form).toString();
    } else if (opts.body !== undefined) {
      headers['Content-Type'] = 'application/json';
      body = JSON.stringify(opts.body);
    }
    if (method !== 'GET') {
      await ensureCsrf();
      const token = getCookie('XSRF-TOKEN');
      if (token) headers['X-XSRF-TOKEN'] = token;
    }

    let res;
    try {
      res = await fetch(path, { method: method, headers: headers, body: body, credentials: 'same-origin' });
    } catch (e) {
      throw new ApiError(0, 'Cannot reach the server. Check that the application is running and try again.');
    }

    // A 403 on a write can mean an out-of-date CSRF token: refresh it and retry once.
    if (res.status === 403 && method !== 'GET' && !opts._retried) {
      await refreshCsrf();
      return api(path, Object.assign({}, opts, { _retried: true }));
    }

    let data = null;
    const text = await res.text();
    if (text) { try { data = JSON.parse(text); } catch (e) { data = null; } }

    if (!res.ok) {
      if (res.status === 401 && !opts.noRedirect) redirectToLogin();
      throw new ApiError(res.status, (data && data.message) || defaultMessage(res.status), data && data.fieldErrors);
    }
    return data;
  }

  /* ---------- session helpers ---------- */
  function fillUser(user) {
    $$('[data-user-name]').forEach(el => { el.textContent = user.fullName || user.email; });
    $$('[data-user-role]').forEach(el => { el.textContent = user.role === 'ADMIN' ? 'Administrator' : 'Alumnus'; });
    $$('[data-user-avatar]').forEach(el => {
      el.textContent = initials(user.fullName || user.email);
      el.className = 'avatar ' + (user.role === 'ADMIN' ? 'c3' : avatarClass(user.fullName));
    });
  }
  async function requireAuth(role) {
    let user;
    try {
      user = await api('/api/auth/me');
    } catch (e) {
      if (e.status === 401) return never();   // api() already redirected to login
      toast(e.message, 'error');
      return null;
    }
    if (role === 'ADMIN' && user.role !== 'ADMIN') { location.replace('dashboard.html'); return never(); }
    if (role === 'ALUMNI' && user.role === 'ADMIN') { location.replace('admin.html'); return never(); }
    fillUser(user);
    return user;
  }
  async function logout() {
    try { await api('/api/auth/logout', { method: 'POST', noRedirect: true }); } catch (e) { /* ignore */ }
    await refreshCsrf();
    location.href = 'login.html?loggedout=1';
  }
  function bindLogout() {
    $$('[data-logout]').forEach(btn => {
      if (btn.dataset.bound) return;
      btn.dataset.bound = '1';
      btn.addEventListener('click', logout);
    });
  }

  /* =================================================================
     4. SHARED UI HELPERS
     ================================================================= */
  function toast(message, type) {
    let box = $('.toast-container');
    if (!box) {
      box = document.createElement('div');
      box.className = 'toast-container';
      box.setAttribute('aria-live', 'polite');
      document.body.appendChild(box);
    }
    const t = document.createElement('div');
    t.className = 'toast' + (type === 'error' ? ' error' : '');
    t.textContent = message;
    box.appendChild(t);
    setTimeout(() => t.remove(), 4500);
  }
  function setLoading(btn, on) {
    if (!btn) return;
    btn.classList.toggle('is-loading', on);
    btn.disabled = on;
  }
  function showAlert(el, msg) { if (el) { el.textContent = msg; el.hidden = false; } }
  function hideAlert(el) { if (el) { el.textContent = ''; el.hidden = true; } }

  /* ---------- form errors ---------- */
  function setFieldError(form, name, msg) {
    const input = form.elements[name];
    const holder = $('[data-error-for="' + name + '"]', form);
    if (!input || !holder) return false;
    const field = input.closest('.field');
    if (msg) {
      holder.textContent = msg;
      if (field) field.classList.add('has-error');
      input.setAttribute('aria-invalid', 'true');
    } else {
      holder.textContent = '';
      if (field) field.classList.remove('has-error');
      input.removeAttribute('aria-invalid');
    }
    return true;
  }
  function clearErrors(form) {
    $$('[data-error-for]', form).forEach(p => setFieldError(form, p.dataset.errorFor, ''));
  }
  function applyServerErrors(form, fieldErrors) {
    const unmatched = [];
    Object.keys(fieldErrors || {}).forEach(name => {
      if (!setFieldError(form, name, fieldErrors[name])) unmatched.push(fieldErrors[name]);
    });
    return unmatched;
  }
  function handleFormError(err, form, alertBox) {
    const fe = err.fieldErrors || {};
    const unmatched = applyServerErrors(form, fe);
    const hasFE = Object.keys(fe).length > 0;
    if (err.status === 409 && !hasFE && form.elements.email) {
      setFieldError(form, 'email', err.message || 'This email address is already registered.');
      showAlert(alertBox, 'Please correct the highlighted fields.');
      return;
    }
    showAlert(alertBox, unmatched.length ? unmatched.join(' ') : hasFE ? 'Please correct the highlighted fields.' : err.message);
  }

  /* ---------- validators (same rules as the Java side) ---------- */
  const V = {
    required: label => v => (String(v).trim() ? '' : label + ' is required.'),
    maxLen: (label, n) => v => (String(v).trim().length <= n ? '' : label + ' must be at most ' + n + ' characters.'),
    email: v => {
      v = String(v).trim();
      if (!v) return 'Email address is required.';
      return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(v) ? '' : 'Enter a valid email address.';
    },
    password: v => {
      if (!v) return 'Password is required.';
      if (v.length < 8 || v.length > 64) return 'Password must be 8–64 characters.';
      if (!/[A-Za-z]/.test(v) || !/\d/.test(v)) return 'Password needs at least one letter and one number.';
      return '';
    },
    phone: v => {
      v = String(v).trim();
      return !v || /^[+]?[0-9 ()-]{7,20}$/.test(v) ? '' : 'Enter a valid phone number (digits, spaces, + ( ) - only).';
    },
    year: v => {
      v = String(v).trim();
      if (!v) return 'Graduation year is required.';
      const n = Number(v);
      if (!/^\d{4}$/.test(v) || n < MIN_YEAR || n > MAX_YEAR) return 'Enter a year between ' + MIN_YEAR + ' and ' + MAX_YEAR + '.';
      return '';
    },
    linkedin: v => {
      v = String(v).trim();
      return !v || /^https?:\/\/([a-z]{2,3}\.)?linkedin\.com\/\S+$/i.test(v) ? '' : 'Enter a valid LinkedIn URL, e.g. https://www.linkedin.com/in/your-name';
    }
  };
  function all() {
    const fns = Array.from(arguments);
    return (v, form) => { for (const fn of fns) { const m = fn(v, form); if (m) return m; } return ''; };
  }
  function validate(form, rules) {
    clearErrors(form);
    let first = null;
    Object.keys(rules).forEach(name => {
      const el = form.elements[name];
      if (!el) return;
      const msg = rules[name](el.type === 'checkbox' ? el.checked : el.value, form);
      if (msg) { setFieldError(form, name, msg); if (!first) first = el; }
    });
    if (first) first.focus();
    return !first;
  }

  /* ---------- selects and profile forms ---------- */
  function fillSelect(select, items, placeholder) {
    select.innerHTML = '';
    select.add(new Option(placeholder, ''));
    items.forEach(i => select.add(new Option(i, i)));
  }
  function setSelectValue(select, value) {
    if (value && !Array.from(select.options).some(o => o.value === value)) select.add(new Option(value, value));
    select.value = value || '';
  }
  function populateForm(form, data) {
    PROFILE_FIELDS.forEach(name => {
      const el = form.elements[name];
      if (!el) return;
      const v = data[name] == null ? '' : data[name];
      if (el.tagName === 'SELECT') setSelectValue(el, v); else el.value = v;
    });
    if (form.elements.showInDirectory) form.elements.showInDirectory.checked = data.showInDirectory !== false;
  }
  function profileRules() {
    return {
      fullName: all(V.required('Full name'), V.maxLen('Full name', 100)),
      phone: V.phone,
      department: V.required('Department'),
      degree: V.required('Degree'),
      graduationYear: V.year,
      company: V.maxLen('Company', 100),
      jobTitle: V.maxLen('Job title', 100),
      city: V.maxLen('City', 80),
      linkedinUrl: V.linkedin,
      bio: V.maxLen('Bio', 500)
    };
  }
  function collectProfile(form) {
    const g = n => form.elements[n].value.trim();
    return {
      fullName: g('fullName'),
      phone: nullIfEmpty(g('phone')),
      department: g('department'),
      degree: g('degree'),
      graduationYear: Number(g('graduationYear')),
      company: nullIfEmpty(g('company')),
      jobTitle: nullIfEmpty(g('jobTitle')),
      city: nullIfEmpty(g('city')),
      linkedinUrl: nullIfEmpty(g('linkedinUrl')),
      bio: nullIfEmpty(g('bio')),
      showInDirectory: form.elements.showInDirectory.checked
    };
  }

  /* ---------- badges ---------- */
  function typeBadge(t) { return '<span class="badge badge-navy">' + esc(TYPE_LABELS[t] || t) + '</span>'; }
  function statusBadge(s) {
    const m = STATUS_META[s] || { label: s, cls: 'badge-muted' };
    return '<span class="badge ' + m.cls + '">' + esc(m.label) + '</span>';
  }
  function amountText(c) { return c.type === 'DONATION' && c.amount != null ? formatMoney(c.amount) : '—'; }

  /* ---------- list view states and pagers ---------- */
  function setView(els, name) {
    els.loading.hidden = name !== 'loading';
    els.error.hidden = name !== 'error';
    els.empty.hidden = name !== 'empty';
    els.content.hidden = name !== 'content';
    if (els.pager && name !== 'content') els.pager.nav.hidden = true;
  }
  function updatePager(p, data) {
    p.nav.hidden = data.totalPages <= 1;
    p.info.textContent = 'Page ' + (data.page + 1) + ' of ' + data.totalPages;
    p.prev.disabled = data.page <= 0;
    p.next.disabled = data.page >= data.totalPages - 1;
  }

  /* ---------- modals ---------- */
  let lastFocus = null;
  function focusables(root) {
    return $$('a[href], button:not([disabled]), input:not([disabled]):not([type=hidden]), select:not([disabled]), textarea:not([disabled])', root)
      .filter(el => !el.closest('[hidden]'));
  }
  function openModal(id) {
    const m = document.getElementById(id);
    if (!m) return;
    lastFocus = document.activeElement;
    m.hidden = false;
    document.body.style.overflow = 'hidden';
    const body = $('.modal-body', m);
    const first = body ? focusables(body)[0] : null;
    (first || $('.modal', m)).focus();
  }
  function closeModal(id) {
    const m = document.getElementById(id);
    if (!m) return;
    m.hidden = true;
    if (!$$('.modal-backdrop').some(x => !x.hidden)) document.body.style.overflow = '';
    if (lastFocus && document.contains(lastFocus)) lastFocus.focus();
  }
  function initModals() {
    document.addEventListener('click', e => {
      const closer = e.target.closest('[data-close-modal]');
      if (closer) { const m = closer.closest('.modal-backdrop'); if (m) closeModal(m.id); return; }
      if (e.target.classList && e.target.classList.contains('modal-backdrop')) closeModal(e.target.id);
    });
    document.addEventListener('keydown', e => {
      const open = $$('.modal-backdrop').filter(m => !m.hidden).pop();
      if (!open) return;
      if (e.key === 'Escape') { closeModal(open.id); return; }
      if (e.key === 'Tab') {
        const f = focusables(open);
        if (!f.length) return;
        const first = f[0], last = f[f.length - 1];
        if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
        else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
      }
    });
  }

  /* ---------- global page chrome ---------- */
  function initPasswordToggles() {
    $$('[data-toggle-password]').forEach(btn => {
      btn.addEventListener('click', () => {
        const input = document.getElementById(btn.dataset.togglePassword);
        if (!input) return;
        const show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        btn.setAttribute('aria-pressed', String(show));
        btn.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
        const on = $('.eye-on', btn), off = $('.eye-off', btn);
        if (on) on.toggleAttribute('hidden', show);
        if (off) off.toggleAttribute('hidden', !show);
      });
    });
  }
  function initNavToggle() {
    const btn = $('#navToggle'), nav = $('#siteNav');
    if (!btn || !nav) return;
    btn.addEventListener('click', () => {
      const open = nav.classList.toggle('is-open');
      btn.setAttribute('aria-expanded', String(open));
    });
  }
  function initSidebar() {
    const app = $('#app'), toggle = $('#sidebarToggle'), overlay = $('#sidebarOverlay');
    if (!app || !toggle) return;
    const set = open => {
      app.classList.toggle('sidebar-open', open);
      toggle.setAttribute('aria-expanded', String(open));
    };
    toggle.addEventListener('click', () => set(!app.classList.contains('sidebar-open')));
    if (overlay) overlay.addEventListener('click', () => set(false));
    document.addEventListener('keydown', e => { if (e.key === 'Escape') set(false); });
    $$('.sidebar-link', app).forEach(l => l.addEventListener('click', () => set(false)));
  }
  function initDirectorySearchForms() {
    $$('form[data-directory-search]').forEach(f => {
      f.addEventListener('submit', e => {
        e.preventDefault();
        const q = f.querySelector('input').value.trim();
        location.href = 'directory.html' + (q ? '?q=' + encodeURIComponent(q) : '');
      });
    });
  }
  async function initPublicNav() {
    const box = $('#navAuth');
    if (!box) return;
    try {
      const user = await api('/api/auth/me', { noRedirect: true });
      const admin = user.role === 'ADMIN';
      box.innerHTML = '<a class="btn btn-primary btn-sm" href="' + (admin ? 'admin.html' : 'dashboard.html') + '">' +
        (admin ? 'Admin panel' : 'My dashboard') + '</a>' +
        '<button class="btn btn-secondary btn-sm" type="button" data-logout>Logout</button>';
      bindLogout();
    } catch (e) { /* not signed in: keep Login / Register */ }
  }

  /* =================================================================
     5. PAGES
     ================================================================= */

  /* ---------- Home ---------- */
  async function initHome() {
    try {
      const s = await api('/api/public/stats', { noRedirect: true });
      $$('[data-stat]').forEach(el => {
        const v = s[el.dataset.stat];
        el.textContent = v == null ? '–' : formatNumber(v);
      });
    } catch (e) {
      const note = $('#statsNote');
      if (note) note.textContent = 'Live statistics are unavailable right now. Make sure the server and database are running.';
    }
  }

  /* ---------- Login ---------- */
  function pickRedirect(user, next) {
    if (user.role === 'ADMIN') return 'admin.html';
    if (next && SAFE_NEXT.indexOf(next) !== -1) return next;
    return user.redirect || 'dashboard.html';
  }
  function initLogin() {
    const form = $('#loginForm'), alertBox = $('#formAlert'), notice = $('#formNotice'), btn = $('#submitBtn');
    const params = new URLSearchParams(location.search);
    if (params.get('registered')) { notice.textContent = 'Account created. Please sign in.'; notice.hidden = false; }
    else if (params.get('loggedout')) { notice.textContent = 'You have been signed out.'; notice.hidden = false; }

    api('/api/auth/me', { noRedirect: true })
      .then(u => location.replace(u.role === 'ADMIN' ? 'admin.html' : 'dashboard.html'))
      .catch(() => {});

    form.addEventListener('submit', async e => {
      e.preventDefault();
      hideAlert(alertBox);
      notice.hidden = true;
      const ok = validate(form, { email: V.email, password: v => (v ? '' : 'Password is required.') });
      if (!ok) return;
      setLoading(btn, true);
      try {
        const user = await api('/api/auth/login', {
          method: 'POST', noRedirect: true,
          form: { email: form.elements.email.value.trim(), password: form.elements.password.value }
        });
        await refreshCsrf(); // the session changed, so the CSRF token was replaced
        location.href = pickRedirect(user, params.get('next'));
      } catch (err) {
        showAlert(alertBox, err.status === 401 ? 'Incorrect email or password.' : err.message);
        setLoading(btn, false);
      }
    });
  }

  /* ---------- Register ---------- */
  function initRegister() {
    const form = $('#registerForm'), alertBox = $('#formAlert'), btn = $('#submitBtn');
    const rules = {
      fullName: all(V.required('Full name'), V.maxLen('Full name', 100)),
      email: V.email,
      phone: V.phone,
      department: V.required('Department'),
      degree: V.required('Degree'),
      graduationYear: V.year,
      password: V.password,
      confirmPassword: (v, f) => (!v ? 'Please confirm your password.' : v !== f.elements.password.value ? 'Passwords do not match.' : '')
    };
    form.addEventListener('submit', async e => {
      e.preventDefault();
      hideAlert(alertBox);
      if (!validate(form, rules)) return;
      const g = n => form.elements[n].value.trim();
      const payload = {
        fullName: g('fullName'), email: g('email'), phone: nullIfEmpty(g('phone')),
        department: g('department'), degree: g('degree'), graduationYear: Number(g('graduationYear')),
        password: form.elements.password.value, confirmPassword: form.elements.confirmPassword.value
      };
      setLoading(btn, true);
      try {
        await api('/api/auth/register', { method: 'POST', body: payload, noRedirect: true });
        location.href = 'login.html?registered=1';
      } catch (err) {
        handleFormError(err, form, alertBox);
        setLoading(btn, false);
      }
    });
  }

  /* ---------- Directory ---------- */
  function alumniCard(a) {
    const li = safeUrl(a.linkedinUrl);
    let role;
    if (a.jobTitle && a.company) role = '<strong>' + esc(a.jobTitle) + '</strong> at ' + esc(a.company);
    else if (a.jobTitle || a.company) role = '<strong>' + esc(a.jobTitle || a.company) + '</strong>';
    else role = '<span class="muted">Career details not shared</span>';
    return '<article class="alumni-card">' +
      '<div class="alumni-card-top">' + avatarHtml(a.fullName) +
        '<div style="min-width:0"><div class="alumni-name truncate">' + esc(a.fullName) + '</div>' +
        '<div class="alumni-sub truncate">' + esc(a.degree || '') + '</div></div></div>' +
      '<div class="alumni-role">' + icon('i-briefcase', 'icon-sm') + '<span>' + role + '</span></div>' +
      '<div class="alumni-tags"><span class="badge badge-teal">' + esc(a.department) + '</span>' +
        '<span class="badge badge-navy">Batch of ' + esc(a.graduationYear) + '</span></div>' +
      '<div class="alumni-card-footer">' +
        '<span>' + (a.city ? icon('i-map-pin', 'icon-sm') + ' ' + esc(a.city) : '') + '</span>' +
        (li ? '<a href="' + esc(li) + '" target="_blank" rel="noopener noreferrer" aria-label="LinkedIn profile of ' + esc(a.fullName) + '">' + icon('i-linkedin', 'icon-sm') + ' LinkedIn</a>' : '') +
      '</div></article>';
  }
  function initDirectory() {
    const els = {
      loading: $('#loadingState'), error: $('#errorState'), empty: $('#emptyState'), content: $('#alumniGrid'),
      pager: { nav: $('#pagination'), prev: $('#prevPage'), next: $('#nextPage'), info: $('#pageInfo') }
    };
    const search = $('#searchInput'), dept = $('#departmentFilter'), year = $('#yearFilter');
    const count = $('#resultCount');
    const state = { q: '', department: '', year: '', page: 0, size: 12 };
    let reqId = 0;

    const params = new URLSearchParams(location.search);
    state.q = (params.get('q') || '').trim();
    search.value = state.q;

    async function load() {
      const id = ++reqId;
      setView(els, 'loading');
      const qs = new URLSearchParams({ page: state.page, size: state.size });
      if (state.q) qs.set('q', state.q);
      if (state.department) qs.set('department', state.department);
      if (state.year) qs.set('graduationYear', state.year);
      try {
        const data = await api('/api/alumni?' + qs.toString(), { noRedirect: true });
        if (id !== reqId) return;
        if (!data.content || !data.content.length) {
          count.textContent = '0 alumni found';
          setView(els, 'empty');
          return;
        }
        els.content.innerHTML = data.content.map(alumniCard).join('');
        const from = data.page * data.size + 1;
        const to = data.page * data.size + data.content.length;
        count.textContent = 'Showing ' + from + '–' + to + ' of ' + formatNumber(data.totalElements) + ' alumni';
        setView(els, 'content');
        updatePager(els.pager, data);
      } catch (e) {
        if (id !== reqId) return;
        $('#errorMessage').textContent = e.message;
        count.innerHTML = '&nbsp;';
        setView(els, 'error');
      }
    }
    async function loadFilters() {
      try {
        const f = await api('/api/alumni/filters', { noRedirect: true });
        (f.departments || []).forEach(d => dept.add(new Option(d, d)));
        (f.graduationYears || []).forEach(y => year.add(new Option(String(y), String(y))));
      } catch (e) { /* filters are optional */ }
    }

    search.addEventListener('input', debounce(() => { state.q = search.value.trim(); state.page = 0; load(); }, 300));
    dept.addEventListener('change', () => { state.department = dept.value; state.page = 0; load(); });
    year.addEventListener('change', () => { state.year = year.value; state.page = 0; load(); });
    $('#clearFilters').addEventListener('click', () => {
      search.value = ''; dept.value = ''; year.value = '';
      state.q = ''; state.department = ''; state.year = ''; state.page = 0;
      load();
    });
    els.pager.prev.addEventListener('click', () => { if (state.page > 0) { state.page--; load(); window.scrollTo({ top: 0, behavior: 'smooth' }); } });
    els.pager.next.addEventListener('click', () => { state.page++; load(); window.scrollTo({ top: 0, behavior: 'smooth' }); });
    $('#retryBtn').addEventListener('click', load);

    loadFilters();
    load();
  }

  /* ---------- Dashboard ---------- */
  async function initDashboard() {
    const user = await requireAuth('ALUMNI');
    if (!user) return;
    const loading = $('#dashLoading'), err = $('#dashError'), content = $('#dashContent');

    function render(profile, summary) {
      const first = String(profile.fullName || user.fullName || '').trim().split(/\s+/)[0] || 'there';
      const pct = Number(summary.profileCompletion) || 0;
      $('#greeting').textContent = 'Welcome back, ' + first;
      $('#welcomeSub').textContent = pct < 100
        ? 'Your profile is ' + pct + '% complete. Finish it so classmates can find you.'
        : 'Your profile is complete. Explore the directory or record a contribution.';
      const cta = $('#welcomeCta');
      if (pct < 100) { cta.textContent = 'Complete your profile'; cta.href = 'profile.html'; }
      else { cta.textContent = 'Add a contribution'; cta.href = 'contributions.html'; }

      $('#statAlumni').textContent = formatNumber(summary.alumniCount);
      $('#statContrib').textContent = formatNumber(summary.myContributionCount);
      $('#statCompletion').textContent = pct + '%';
      $('#statBatch').textContent = profile.graduationYear || '–';

      $('#completionBar').style.width = Math.max(0, Math.min(100, pct)) + '%';
      $('#completionTrack').setAttribute('aria-valuenow', String(pct));
      $('#completionText').textContent = pct + '% of profile fields completed';

      $('#profileKv').innerHTML =
        '<dt>Name</dt><dd>' + val(profile.fullName) + '</dd>' +
        '<dt>Email</dt><dd>' + val(profile.email) + '</dd>' +
        '<dt>Department</dt><dd>' + val(profile.department) + '</dd>' +
        '<dt>Degree</dt><dd>' + val(profile.degree) + '</dd>' +
        '<dt>Batch</dt><dd>' + val(profile.graduationYear) + '</dd>' +
        '<dt>Job title</dt><dd>' + val(profile.jobTitle) + '</dd>' +
        '<dt>Company</dt><dd>' + val(profile.company) + '</dd>' +
        '<dt>City</dt><dd>' + val(profile.city) + '</dd>';

      const recent = summary.recentContributions || [];
      const list = $('#recentList'), empty = $('#recentEmpty');
      list.innerHTML = recent.map(c =>
        '<li class="list-item">' +
          '<span class="feature-icon sm ' + (TYPE_ICON_CLASS[c.type] || '') + '">' + icon(TYPE_ICONS[c.type] || 'i-award') + '</span>' +
          '<div class="grow"><div class="title truncate">' + esc(TYPE_LABELS[c.type] || c.type) +
            (c.type === 'DONATION' && c.amount != null ? ' · ' + esc(formatMoney(c.amount)) : '') + '</div>' +
          '<div class="sub truncate">' + esc(formatDate(c.contributionDate)) + ' · ' + esc(c.description) + '</div></div>' +
          statusBadge(c.status) + '</li>').join('');
      list.hidden = recent.length === 0;
      empty.hidden = recent.length !== 0;

      $('#visibilityInfo').textContent = profile.showInDirectory
        ? 'Your profile is visible in the alumni directory. Email and phone are never shown.'
        : 'Your profile is hidden from the directory. You can change this on My Profile.';
    }

    async function load() {
      loading.hidden = false; err.hidden = true; content.hidden = true;
      try {
        const results = await Promise.all([api('/api/profile/me'), api('/api/dashboard/summary')]);
        render(results[0], results[1]);
        loading.hidden = true; content.hidden = false;
      } catch (e) {
        loading.hidden = true;
        $('#dashErrorMsg').textContent = e.message;
        err.hidden = false;
      }
    }
    $('#dashRetry').addEventListener('click', load);
    load();
  }

  /* ---------- Profile ---------- */
  async function initProfile() {
    const user = await requireAuth('ALUMNI');
    if (!user) return;
    const form = $('#profileForm'), alertBox = $('#formAlert'), btn = $('#submitBtn');
    const loading = $('#profileLoading'), error = $('#profileError'), wrap = $('#profileWrap');
    fillSelect(form.elements.department, DEPARTMENTS, 'Select department');
    fillSelect(form.elements.degree, DEGREES, 'Select degree');

    const bio = form.elements.bio, bioCount = $('#bioCount');
    const updateBio = () => { bioCount.textContent = String(bio.value.length); };
    bio.addEventListener('input', updateBio);

    function applyProfile(p) {
      populateForm(form, p);
      updateBio();
      $('#asideAvatar').textContent = initials(p.fullName);
      $('#asideAvatar').className = 'avatar avatar-lg ' + avatarClass(p.fullName);
      $('#asideName').textContent = p.fullName || '';
      $('#asideEmail').textContent = p.email || '';
    }
    async function load() {
      loading.hidden = false; error.hidden = true; wrap.hidden = true;
      try {
        applyProfile(await api('/api/profile/me'));
        loading.hidden = true; wrap.hidden = false;
      } catch (e) {
        loading.hidden = true;
        $('#profileErrorMsg').textContent = e.message;
        error.hidden = false;
      }
    }
    $('#profileRetry').addEventListener('click', load);

    form.addEventListener('submit', async e => {
      e.preventDefault();
      hideAlert(alertBox);
      if (!validate(form, profileRules())) return;
      setLoading(btn, true);
      try {
        const updated = await api('/api/profile/me', { method: 'PUT', body: collectProfile(form) });
        if (updated) applyProfile(updated);
        fillUser({ fullName: form.elements.fullName.value.trim(), email: form.elements.email.value, role: 'ALUMNI' });
        toast('Profile updated.');
      } catch (err) {
        handleFormError(err, form, alertBox);
      }
      setLoading(btn, false);
    });
    load();
  }

  /* ---------- Contributions (alumni) ---------- */
  async function initContributions() {
    const user = await requireAuth('ALUMNI');
    if (!user) return;
    const form = $('#contributionForm'), alertBox = $('#formAlert'), btn = $('#submitBtn');
    const amountField = $('#amountField');
    const dateInput = form.elements.contributionDate;
    dateInput.max = todayString();
    dateInput.value = todayString();

    function toggleAmount() {
      const isDonation = form.elements.type.value === 'DONATION';
      amountField.hidden = !isDonation;
      if (!isDonation) { form.elements.amount.value = ''; setFieldError(form, 'amount', ''); }
    }
    form.elements.type.addEventListener('change', toggleAmount);
    toggleAmount();

    const rules = {
      type: V.required('Contribution type'),
      contributionDate: v => {
        if (!v) return 'Date is required.';
        if (!/^\d{4}-\d{2}-\d{2}$/.test(v)) return 'Enter a valid date.';
        return v > todayString() ? 'The date cannot be in the future.' : '';
      },
      amount: (v, f) => {
        if (f.elements.type.value !== 'DONATION') return '';
        if (String(v).trim() === '') return 'Enter the donation amount.';
        const n = Number(v);
        if (isNaN(n) || n <= 0) return 'Amount must be greater than zero.';
        return n > 100000000 ? 'That amount is too large.' : '';
      },
      description: all(V.required('Description'), V.maxLen('Description', 500))
    };

    const els = { loading: $('#listLoading'), error: $('#listError'), empty: $('#listEmpty'), content: $('#listWrap') };
    async function loadList() {
      setView(els, 'loading');
      try {
        const data = await api('/api/contributions/me');
        const items = (Array.isArray(data) ? data : (data && data.content) || []).slice();
        items.sort((a, b) => String(b.contributionDate).localeCompare(String(a.contributionDate)));
        $('#listCount').textContent = items.length ? items.length + (items.length === 1 ? ' entry' : ' entries') : '';
        if (!items.length) { setView(els, 'empty'); return; }
        $('#listBody').innerHTML = items.map(c =>
          '<tr><td>' + esc(formatDate(c.contributionDate)) + '</td>' +
          '<td>' + typeBadge(c.type) + '</td>' +
          '<td class="desc-cell">' + esc(c.description) + '</td>' +
          '<td>' + esc(amountText(c)) + '</td>' +
          '<td>' + statusBadge(c.status) + '</td></tr>').join('');
        setView(els, 'content');
      } catch (e) {
        $('#listErrorMsg').textContent = e.message;
        setView(els, 'error');
      }
    }
    $('#listRetry').addEventListener('click', loadList);

    form.addEventListener('submit', async e => {
      e.preventDefault();
      hideAlert(alertBox);
      if (!validate(form, rules)) return;
      const isDonation = form.elements.type.value === 'DONATION';
      const payload = {
        type: form.elements.type.value,
        description: form.elements.description.value.trim(),
        amount: isDonation ? Number(form.elements.amount.value) : null,
        contributionDate: form.elements.contributionDate.value
      };
      setLoading(btn, true);
      try {
        await api('/api/contributions', { method: 'POST', body: payload });
        toast('Contribution recorded. An administrator will review it.');
        form.reset();
        dateInput.value = todayString();
        toggleAmount();
        loadList();
      } catch (err) {
        handleFormError(err, form, alertBox);
      }
      setLoading(btn, false);
    });
    loadList();
  }

  /* ---------- Admin ---------- */
  async function initAdmin() {
    const user = await requireAuth('ADMIN');
    if (!user) return;

    const VIEWS = { overview: 'Overview', alumni: 'Alumni', contributions: 'Contributions' };
    const loaded = {};
    const loaders = {};

    function showView(name) {
      if (!VIEWS[name]) name = 'overview';
      $$('[data-view-panel]').forEach(p => { p.hidden = p.dataset.viewPanel !== name; });
      $$('[data-view]').forEach(b => {
        const on = b.dataset.view === name;
        b.classList.toggle('active', on);
        if (on) b.setAttribute('aria-current', 'page'); else b.removeAttribute('aria-current');
      });
      $('#viewTitle').textContent = VIEWS[name];
      if (history.replaceState) history.replaceState(null, '', '#' + name);
      if (!loaded[name]) { loaded[name] = true; loaders[name](); }
    }
    $$('[data-view]').forEach(b => b.addEventListener('click', () => showView(b.dataset.view)));

    /* ----- overview ----- */
    function renderBreakdown(sel, counts, labelOf, total) {
      const box = $(sel);
      box.innerHTML = Object.keys(labelOf).map(key => {
        const n = (counts && counts[key]) || 0;
        const pct = total > 0 ? Math.round((n / total) * 100) : 0;
        return '<div class="breakdown-row"><span>' + esc(labelOf[key]) + '</span>' +
          '<div class="progress" aria-hidden="true"><span style="width:' + pct + '%"></span></div>' +
          '<strong>' + formatNumber(n) + '</strong></div>';
      }).join('');
    }
    loaders.overview = async function () {
      const errBox = $('#statsError');
      errBox.hidden = true;
      try {
        const s = await api('/api/admin/stats');
        $('#aStatAlumni').textContent = formatNumber(s.totalAlumni);
        $('#aStatContrib').textContent = formatNumber(s.totalContributions);
        $('#aStatPending').textContent = formatNumber(s.pendingContributions);
        $('#aStatAmount').textContent = formatMoney(s.verifiedDonationAmount);
        const statusLabels = {};
        Object.keys(STATUS_META).forEach(k => { statusLabels[k] = STATUS_META[k].label; });
        renderBreakdown('#byTypeList', s.byType, TYPE_LABELS, s.totalContributions);
        renderBreakdown('#byStatusList', s.byStatus, statusLabels, s.totalContributions);
      } catch (e) { showAlert(errBox, e.message); }
    };
    $('#refreshStats').addEventListener('click', loaders.overview);

    /* ----- alumni table ----- */
    const aState = { q: '', department: '', page: 0, size: 10 };
    const aEls = {
      loading: $('#aLoading'), error: $('#aError'), empty: $('#aEmpty'), content: $('#aWrap'),
      pager: { nav: $('#aPagination'), prev: $('#aPrev'), next: $('#aNext'), info: $('#aPageInfo') }
    };
    let aItems = new Map(), aReq = 0;
    fillSelect($('#aDepartment'), DEPARTMENTS, 'All departments');

    loaders.alumni = async function () {
      const id = ++aReq;
      setView(aEls, 'loading');
      const qs = new URLSearchParams({ page: aState.page, size: aState.size });
      if (aState.q) qs.set('q', aState.q);
      if (aState.department) qs.set('department', aState.department);
      try {
        const data = await api('/api/admin/alumni?' + qs.toString());
        if (id !== aReq) return;
        aItems = new Map((data.content || []).map(a => [a.id, a]));
        if (!data.content || !data.content.length) { setView(aEls, 'empty'); return; }
        $('#aBody').innerHTML = data.content.map(a =>
          '<tr><td><div class="cell-user">' + avatarHtml(a.fullName, 'sm') +
            '<div style="min-width:0"><strong class="truncate" style="display:block">' + esc(a.fullName) + '</strong>' +
            '<div class="sub truncate">' + esc(a.email) + '</div></div></div></td>' +
          '<td>' + esc(a.department) + '</td>' +
          '<td>' + esc(a.graduationYear) + '</td>' +
          '<td>' + val(a.jobTitle) + '<div class="sub" style="font-size:.8rem;color:var(--text-muted)">' + esc(a.company || '') + '</div></td>' +
          '<td>' + val(a.city) + '</td>' +
        
'<td><div class="actions">' +
  '<button class="icon-btn" type="button" data-action="view" data-id="' + esc(a.id) + '" aria-label="View ' + esc(a.fullName) + '" title="View Details">' +
    icon('i-eye', 'icon-sm') + ' <span>View</span>' +
  '</button>' +
  '<button class="icon-btn" type="button" data-action="edit" data-id="' + esc(a.id) + '" aria-label="Edit ' + esc(a.fullName) + '" title="Edit Alumni">' +
    icon('i-edit', 'icon-sm') + ' <span>Edit</span>' +
  '</button>' +
  '<button class="icon-btn danger" type="button" data-action="delete" data-id="' + esc(a.id) + '" aria-label="Delete ' + esc(a.fullName) + '" title="Delete Alumni">' +
    icon('i-trash', 'icon-sm') + ' <span>Delete</span>' +
  '</button>' +
'</div></td></tr>').join('');

        setView(aEls, 'content');
        updatePager(aEls.pager, data);
      } catch (e) {
        if (id !== aReq) return;
        $('#aErrorMsg').textContent = e.message;
        setView(aEls, 'error');
      }
    };
    $('#aSearch').addEventListener('input', debounce(e => { aState.q = e.target.value.trim(); aState.page = 0; loaders.alumni(); }, 300));
    $('#aDepartment').addEventListener('change', e => { aState.department = e.target.value; aState.page = 0; loaders.alumni(); });
    $('#aClear').addEventListener('click', () => {
      $('#aSearch').value = ''; $('#aDepartment').value = '';
      aState.q = ''; aState.department = ''; aState.page = 0; loaders.alumni();
    });
    aEls.pager.prev.addEventListener('click', () => { if (aState.page > 0) { aState.page--; loaders.alumni(); } });
    aEls.pager.next.addEventListener('click', () => { aState.page++; loaders.alumni(); });
    $('#aRetry').addEventListener('click', loaders.alumni);

    $('#aBody').addEventListener('click', e => {
      const b = e.target.closest('[data-action]');
      if (!b) return;
      const a = aItems.get(b.dataset.id);
      if (!a) return;
      if (b.dataset.action === 'view') openView(a);
      else if (b.dataset.action === 'edit') openAlumniForm(a);
      else if (b.dataset.action === 'delete') openDelete(a);
    });

    /* ----- view modal ----- */
    async function openView(a) {
      $('#viewBody').innerHTML =
        '<div class="cell-user" style="margin-bottom:1.25rem">' + avatarHtml(a.fullName, 'lg') +
          '<div><h3>' + esc(a.fullName) + '</h3><div class="muted">' + esc(a.email) + '</div></div></div>' +
        '<dl class="kv">' +
          '<dt>Phone</dt><dd>' + val(a.phone) + '</dd>' +
          '<dt>Department</dt><dd>' + val(a.department) + '</dd>' +
          '<dt>Degree</dt><dd>' + val(a.degree) + '</dd>' +
          '<dt>Batch</dt><dd>' + val(a.graduationYear) + '</dd>' +
          '<dt>Job title</dt><dd>' + val(a.jobTitle) + '</dd>' +
          '<dt>Company</dt><dd>' + val(a.company) + '</dd>' +
          '<dt>City</dt><dd>' + val(a.city) + '</dd>' +
          '<dt>LinkedIn</dt><dd>' + (safeUrl(a.linkedinUrl) ? '<a href="' + esc(safeUrl(a.linkedinUrl)) + '" target="_blank" rel="noopener noreferrer">' + esc(a.linkedinUrl) + '</a>' : '—') + '</dd>' +
          '<dt>Bio</dt><dd>' + val(a.bio) + '</dd>' +
          '<dt>In directory</dt><dd>' + (a.showInDirectory ? 'Yes' : 'No') + '</dd>' +
          '<dt>Joined</dt><dd>' + esc(formatDate(a.createdAt)) + '</dd>' +
        '</dl>' +
        '<h3 style="margin:1.5rem 0 .25rem">Contributions</h3><div id="viewContribs"><p class="muted">Loading…</p></div>';
      $('#viewEditBtn').dataset.id = a.id;
      openModal('viewModal');
      try {
        const data = await api('/api/admin/contributions?alumniId=' + encodeURIComponent(a.id) + '&size=50');
        const items = data.content || [];
        $('#viewContribs').innerHTML = items.length
          ? '<ul class="list">' + items.map(c =>
              '<li class="list-item"><div class="grow"><div class="title">' + esc(TYPE_LABELS[c.type] || c.type) +
              (c.type === 'DONATION' && c.amount != null ? ' · ' + esc(formatMoney(c.amount)) : '') + '</div>' +
              '<div class="sub">' + esc(formatDate(c.contributionDate)) + ' · ' + esc(c.description) + '</div></div>' +
              statusBadge(c.status) + '</li>').join('') + '</ul>'
          : '<p class="muted">No contributions recorded.</p>';
      } catch (e) {
        $('#viewContribs').innerHTML = '<p class="muted">Could not load contributions: ' + esc(e.message) + '</p>';
      }
    }
    $('#viewEditBtn').addEventListener('click', e => {
      const a = aItems.get(e.currentTarget.dataset.id);
      closeModal('viewModal');
      if (a) openAlumniForm(a);
    });

    /* ----- add / edit modal ----- */
    const aForm = $('#alumniForm'), aAlert = $('#alumniAlert'), aSubmit = $('#alumniSubmit');
    let editingId = null;
    fillSelect(aForm.elements.department, DEPARTMENTS, 'Select department');
    fillSelect(aForm.elements.degree, DEGREES, 'Select degree');

    function openAlumniForm(a) {
      editingId = a ? a.id : null;
      $('#alumniModalTitle').textContent = a ? 'Edit alumni' : 'Add alumni';
      aForm.reset();
      clearErrors(aForm);
      hideAlert(aAlert);
      if (a) populateForm(aForm, a); else { populateForm(aForm, {}); aForm.elements.showInDirectory.checked = true; }
      $('#pwField').hidden = !!a;
      aForm.elements.temporaryPassword.value = '';
      openModal('alumniModal');
    }
    $('#addAlumniBtn').addEventListener('click', () => openAlumniForm(null));

    aForm.addEventListener('submit', async e => {
      e.preventDefault();
      hideAlert(aAlert);
      const rules = profileRules();
      rules.email = V.email;
      rules.temporaryPassword = v => (editingId ? '' : V.password(v));
      if (!validate(aForm, rules)) return;
      const payload = collectProfile(aForm);
      payload.email = aForm.elements.email.value.trim().toLowerCase();
      if (!editingId) payload.temporaryPassword = aForm.elements.temporaryPassword.value;
      setLoading(aSubmit, true);
      try {
        await api(editingId ? '/api/admin/alumni/' + encodeURIComponent(editingId) : '/api/admin/alumni', {
          method: editingId ? 'PUT' : 'POST', body: payload
        });
        closeModal('alumniModal');
        toast(editingId ? 'Alumni record updated.' : 'Alumni record created.');
        loaded.overview = false;
        loaders.alumni();
      } catch (err) {
        handleFormError(err, aForm, aAlert);
      }
      setLoading(aSubmit, false);
    });

    /* ----- delete modal ----- */
    let pendingDelete = null;
    const confirmBtn = $('#confirmDelete');
    function openDelete(a) {
      pendingDelete = a;
      $('#deleteName').textContent = a.fullName;
      openModal('deleteModal');
    }
    confirmBtn.addEventListener('click', async () => {
      if (!pendingDelete) return;
      setLoading(confirmBtn, true);
      try {
        await api('/api/admin/alumni/' + encodeURIComponent(pendingDelete.id), { method: 'DELETE' });
        closeModal('deleteModal');
        toast('Alumni record deleted.');
        if (aItems.size === 1 && aState.page > 0) aState.page--;
        pendingDelete = null;
        loaded.overview = false;
        loaded.contributions = false;
        loaders.alumni();
      } catch (err) {
        closeModal('deleteModal');
        toast(err.message, 'error');
      }
      setLoading(confirmBtn, false);
    });

    /* ----- contributions table ----- */
    const cState = { type: '', status: '', page: 0, size: 10 };
    const cEls = {
      loading: $('#cLoading'), error: $('#cError'), empty: $('#cEmpty'), content: $('#cWrap'),
      pager: { nav: $('#cPagination'), prev: $('#cPrev'), next: $('#cNext'), info: $('#cPageInfo') }
    };
    let cReq = 0;
    function statusSelect(c) {
      return '<select class="select status-select" data-id="' + esc(c.id) + '" data-current="' + esc(c.status) + '" aria-label="Status of contribution by ' + esc(c.alumniName || 'alumnus') + '">' +
        Object.keys(STATUS_META).map(k => '<option value="' + k + '"' + (k === c.status ? ' selected' : '') + '>' + esc(STATUS_META[k].label) + '</option>').join('') +
        '</select>';
    }
    loaders.contributions = async function () {
      const id = ++cReq;
      setView(cEls, 'loading');
      const qs = new URLSearchParams({ page: cState.page, size: cState.size });
      if (cState.type) qs.set('type', cState.type);
      if (cState.status) qs.set('status', cState.status);
      try {
        const data = await api('/api/admin/contributions?' + qs.toString());
        if (id !== cReq) return;
        if (!data.content || !data.content.length) { setView(cEls, 'empty'); return; }
        $('#cBody').innerHTML = data.content.map(c =>
          '<tr><td>' + esc(formatDate(c.contributionDate)) + '</td>' +
          '<td>' + esc(c.alumniName || '—') + '</td>' +
          '<td>' + typeBadge(c.type) + '</td>' +
          '<td class="desc-cell">' + esc(c.description) + '</td>' +
          '<td>' + esc(amountText(c)) + '</td>' +
          '<td>' + statusSelect(c) + '</td></tr>').join('');
        setView(cEls, 'content');
        updatePager(cEls.pager, data);
      } catch (e) {
        if (id !== cReq) return;
        $('#cErrorMsg').textContent = e.message;
        setView(cEls, 'error');
      }
    };
    $('#cType').addEventListener('change', e => { cState.type = e.target.value; cState.page = 0; loaders.contributions(); });
    $('#cStatus').addEventListener('change', e => { cState.status = e.target.value; cState.page = 0; loaders.contributions(); });
    $('#cClear').addEventListener('click', () => {
      $('#cType').value = ''; $('#cStatus').value = '';
      cState.type = ''; cState.status = ''; cState.page = 0; loaders.contributions();
    });
    cEls.pager.prev.addEventListener('click', () => { if (cState.page > 0) { cState.page--; loaders.contributions(); } });
    cEls.pager.next.addEventListener('click', () => { cState.page++; loaders.contributions(); });
    $('#cRetry').addEventListener('click', loaders.contributions);

    $('#cBody').addEventListener('change', async e => {
      const sel = e.target.closest('.status-select');
      if (!sel) return;
      const previous = sel.dataset.current;
      sel.disabled = true;
      try {
        await api('/api/admin/contributions/' + encodeURIComponent(sel.dataset.id) + '/status', {
          method: 'PATCH', body: { status: sel.value }
        });
        sel.dataset.current = sel.value;
        toast('Status updated to ' + STATUS_META[sel.value].label + '.');
        loaded.overview = false;
      } catch (err) {
        sel.value = previous;
        toast(err.message, 'error');
      }
      sel.disabled = false;
    });

    /* ----- start on the view named in the URL hash ----- */
    showView((location.hash || '').replace('#', '') || 'overview');
  }

  /* =================================================================
     6. START
     ================================================================= */
  function init() {
    const yr = String(new Date().getFullYear());
    $$('[data-year]').forEach(el => { el.textContent = yr; });
    initPasswordToggles();
    initNavToggle();
    initSidebar();
    initModals();
    initDirectorySearchForms();
    bindLogout();

    const page = document.body.dataset.page;
    const pages = {
      home: initHome, directory: initDirectory, login: initLogin, register: initRegister,
      dashboard: initDashboard, profile: initProfile, contributions: initContributions, admin: initAdmin
    };
    if (pages[page]) {
      Promise.resolve(pages[page]()).catch(err => {
        console.error(err);
        toast('Something went wrong loading this page.', 'error');
      });
    }
    if (page === 'home' || page === 'directory') initPublicNav();
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})();