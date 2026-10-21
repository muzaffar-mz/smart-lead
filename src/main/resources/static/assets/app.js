(() => {
  const $ = (s, r = document) => r.querySelector(s);
  const KEY = 'smartlead.apiKey';
  const ADMIN_TABS = ['leads', 'messages', 'completions'];
  const state = { tab: 'submit', page: 0, totalPages: 0, size: 12 };
  const store = {
    get: () => { try { return sessionStorage.getItem(KEY) || ''; } catch { return ''; } },
    set: v => { try { sessionStorage.setItem(KEY, v); } catch { /* storage blocked */ } },
  };

  const LEAD_TYPES = ['DEMO_REQUEST', 'PRICING_INQUIRY', 'PARTNERSHIP', 'SUPPORT', 'OTHER'];
  const URGENCIES = ['HIGH', 'MEDIUM', 'LOW'];
  const pretty = s => s ? s.toLowerCase().replace(/_/g, ' ').replace(/^./, c => c.toUpperCase()) : '';
  const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const when = s => s ? new Date(s).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : '';

  function toast(msg) {
    const t = $('#toast'); t.textContent = msg; t.classList.add('show');
    clearTimeout(toast.t); toast.t = setTimeout(() => t.classList.remove('show'), 3500);
  }

  async function api(path, params = {}, opts = {}) {
    const q = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => { if (v !== '' && v != null) q.set(k, v); });
    const url = (opts.public ? '/api/public/' : '/api/v1/') + path + (q.toString() ? '?' + q : '');
    const res = await fetch(url, { ...opts, headers: { ...(store.get() ? { 'X-API-KEY': store.get() } : {}), 'Content-Type': 'application/json' } });
    let body = null;
    try { body = await res.json(); } catch { /* no body */ }
    if (res.status === 401 && store.get()) { logout(); throw new Error('Session expired, please log in again'); }
    if (!res.ok) throw new Error(body?.error_message || 'Request failed (' + res.status + ')');
    return body;
  }

  // ---------- tabs ----------
  function showTab(tab) {
    state.tab = tab; state.page = 0;
    document.querySelectorAll('#tabs button').forEach(b => b.classList.toggle('active', b.dataset.tab === tab));
    document.querySelectorAll('.view').forEach(v => v.classList.toggle('active', v.id === 'view-' + tab));
    $('#pager').hidden = tab === 'submit';
    $('#stats').hidden = tab === 'submit';
    load();
  }

  // ---------- loaders ----------
  async function load() {
    if (state.tab === 'submit') return;
    if (!store.get()) { showTab('submit'); return; }
    try {
      if (state.tab === 'leads') await loadLeads();
      else if (state.tab === 'messages') await loadMessages();
      else if (state.tab === 'completions') await loadCompletions();
      loadStats();
    } catch (e) { toast(e.message); }
  }

  const sizeParams = () => ({ page: state.page, size: state.size });

  async function loadLeads() {
    const [sortBy, direction] = $('#lead-sort').value.split('|');
    const r = (await api('leads', { ...sizeParams(), type: $('#lead-type').value, urgency: $('#lead-urgency').value, sortBy, direction })).payload;
    $('#leads-list').innerHTML = r.content.length ? r.content.map(l => `
      <article class="card ${l.urgency.toLowerCase()}" data-id="${l.id}">
        <h4>${esc(l.title)}</h4>
        <p>${esc(l.summary)}</p>
        <div class="meta"><span class="badge">${pretty(l.type)}</span><span class="badge ${l.urgency.toLowerCase()}">${pretty(l.urgency)}</span>
          <span>${when(l.dateCreated)}</span></div>
      </article>`).join('') : '<div class="empty">No leads yet. Send a message and wait for the next run.</div>';
    pager(r);
  }

  async function loadMessages() {
    const r = (await api('messages', { ...sizeParams(), email: $('#msg-email').value.trim(), phone: $('#msg-phone').value.trim(), sortBy: 'id', direction: 'desc' })).payload;
    $('#messages-list').innerHTML = table(['#', 'Contact', 'Message', 'Received'], r.content.map(m =>
      `<tr data-id="${m.id}"><td>${m.id}</td><td>${esc(m.email || m.phone)}</td><td class="text">${esc(m.message)}</td><td>${when(m.dateCreated)}</td></tr>`), r.content.length);
    pager(r);
  }

  async function loadCompletions() {
    const r = (await api('completions', { ...sizeParams(), status: $('#cmp-status').value, sortBy: 'id', direction: 'desc' })).payload;
    $('#completions-list').innerHTML = table(['#', 'Message', 'Status', 'Model', 'Tokens', 'Updated'], r.content.map(c =>
      `<tr data-id="${c.id}"><td>${c.id}</td><td>#${c.messageId}</td><td><span class="badge ${c.status.toLowerCase()}">${c.status}</span></td>
        <td>${esc(c.model)}</td><td>${c.usage?.totalTokens ?? ''}</td><td>${when(c.dateUpdated)}</td></tr>`), r.content.length);
    pager(r);
  }

  function table(head, rows, n) {
    if (!n) return '<div class="empty">Nothing here yet.</div>';
    return `<table><thead><tr>${head.map(h => `<th>${h}</th>`).join('')}</tr></thead><tbody>${rows.join('')}</tbody></table>`;
  }

  async function loadStats() {
    try {
      const count = async (p, extra = {}) => (await api(p, { size: 1, ...extra })).payload.total_elements;
      const [leads, high, messages, pending, errors] = await Promise.all([
        count('leads'), count('leads', { urgency: 'HIGH' }), count('messages'),
        count('completions', { status: 'PENDING' }), count('completions', { status: 'ERROR' })]);
      $('#stats').innerHTML = [['Leads', leads], ['High urgency', high], ['Messages', messages], ['Waiting for model', pending], ['Errors', errors]]
        .map(([l, n]) => `<div class="stat"><div class="n">${n}</div><div class="l">${l}</div></div>`).join('');
    } catch { /* shown by main load */ }
  }

  function pager(r) {
    state.totalPages = r.total_pages;
    $('#pager').innerHTML = r.total_pages > 1 ? `
      <button class="ghost" id="prev" ${r.page <= 0 ? 'disabled' : ''}>Previous</button>
      <span>Page ${r.page + 1} of ${r.total_pages} &middot; ${r.total_elements} total</span>
      <button class="ghost" id="next" ${r.page + 1 >= r.total_pages ? 'disabled' : ''}>Next</button>` : '';
  }

  // ---------- detail drawer ----------
  function openDrawer(title, html) {
    $('#drawer-title').textContent = title; $('#drawer-body').innerHTML = html;
    $('#drawer').classList.add('open'); $('#scrim').classList.add('open');
  }
  function closeDrawer() { $('#drawer').classList.remove('open'); $('#scrim').classList.remove('open'); }

  const kv = rows => `<dl class="kv">${rows.filter(r => r[1] != null && r[1] !== '').map(([k, v]) => `<dt>${k}</dt><dd>${esc(v)}</dd>`).join('')}</dl>`;
  const messageBlock = m => `<h5>Message #${m.id}</h5>${kv([['Contact', m.email || m.phone], ['IP', m.ip], ['Received', when(m.dateCreated)]])}<h5>Text</h5><pre>${esc(m.message)}</pre>`;
  const completionBlock = c => `<h5>Model run #${c.id}</h5>${kv([['Status', c.status], ['Model', c.model],
      ['Tokens', c.usage?.totalTokens], ['Reasoning tokens', c.usage?.reasoningTokens]])}
      ${c.content ? `<h5>Answer</h5><pre>${esc(c.content)}</pre>` : ''}${c.reasoning ? `<h5>Reasoning</h5><pre>${esc(c.reasoning)}</pre>` : ''}`;

  async function showLead(id) {
    const lead = (await api('leads/' + id)).payload;
    const [msg, cmp] = await Promise.all([api('messages/' + lead.messageId), api('completions/message/' + lead.messageId).catch(() => null)]);
    openDrawer(lead.title, `<p>${esc(lead.summary)}</p>
      <span class="badge">${pretty(lead.type)}</span> <span class="badge ${lead.urgency.toLowerCase()}">${pretty(lead.urgency)}</span>
      ${messageBlock(msg.payload)}${cmp ? completionBlock(cmp.payload) : ''}`);
  }
  async function showMessage(id) {
    const [msg, cmp] = await Promise.all([api('messages/' + id), api('completions/message/' + id).catch(() => null)]);
    openDrawer('Message #' + id, messageBlock(msg.payload) + (cmp ? completionBlock(cmp.payload) : '<h5>Model run</h5><p class="muted">Not queued yet.</p>'));
  }
  async function showCompletion(id) {
    const c = (await api('completions/' + id)).payload;
    const msg = await api('messages/' + c.messageId).catch(() => null);
    openDrawer('Completion #' + id, completionBlock(c) + (c.status === 'ERROR' ? `<h5>Error</h5><pre>${esc(c.message)}</pre>` : '') + (msg ? messageBlock(msg.payload) : ''));
  }

  // ---------- admin login ----------
  function applyAdminUi() {
    const admin = !!store.get();
    $('#tabs').hidden = !admin; $('#logout-btn').hidden = !admin; $('#admin-btn').hidden = admin;
  }
  function logout() { store.set(''); applyAdminUi(); closeDrawer(); showTab('submit'); }
  function openLogin() { const d = $('#login-dialog'); if (!d.open) { $('#login-error').textContent = ''; $('#login-form').reset(); d.showModal(); } }

  // ---------- events ----------
  function init() {
    $('#lead-type').innerHTML += LEAD_TYPES.map(t => `<option value="${t}">${pretty(t)}</option>`).join('');
    $('#lead-urgency').innerHTML += URGENCIES.map(t => `<option value="${t}">${pretty(t)}</option>`).join('');

    $('#tabs').addEventListener('click', e => { const b = e.target.closest('button'); if (b) showTab(b.dataset.tab); });
    document.addEventListener('click', e => {
      if (e.target.closest('[data-refresh]')) load();
      if (e.target.id === 'prev') { state.page--; load(); }
      if (e.target.id === 'next') { state.page++; load(); }
    });
    ['#lead-type', '#lead-urgency', '#lead-sort', '#cmp-status'].forEach(s => $(s).addEventListener('change', () => { state.page = 0; load(); }));
    let t; ['#msg-email', '#msg-phone'].forEach(s => $(s).addEventListener('input', () => { clearTimeout(t); t = setTimeout(() => { state.page = 0; load(); }, 300); }));

    const open = (sel, fn) => $(sel).addEventListener('click', e => { const r = e.target.closest('[data-id]'); if (r) fn(r.dataset.id).catch(err => toast(err.message)); });
    open('#leads-list', showLead); open('#messages-list', showMessage); open('#completions-list', showCompletion);
    $('#drawer-close').addEventListener('click', closeDrawer); $('#scrim').addEventListener('click', closeDrawer);
    document.addEventListener('keydown', e => { if (e.key === 'Escape') closeDrawer(); });

    $('#admin-btn').addEventListener('click', openLogin);
    $('#logout-btn').addEventListener('click', logout);
    $('#login-cancel').addEventListener('click', () => $('#login-dialog').close());
    $('#login-form').addEventListener('submit', async e => {
      e.preventDefault();
      try {
        const r = await api('admin/login', {}, { method: 'POST', public: true, body: JSON.stringify({ login: $('#login-user').value, password: $('#login-pass').value }) });
        store.set(r.payload.apiKey); $('#login-dialog').close(); applyAdminUi(); showTab('leads');
      } catch (err) { $('#login-error').textContent = err.message; }
    });

    $('#submit-form').addEventListener('submit', async e => {
      e.preventDefault();
      const btn = e.target.querySelector('button[type=submit]'); btn.disabled = true;
      try {
        const r = await api('messages', {}, { method: 'POST', body: JSON.stringify({ contact: $('#sub-contact').value.trim(), message: $('#sub-message').value.trim() }) });
        $('#sub-result').textContent = r.message; e.target.reset();
      } catch (err) { $('#sub-result').textContent = err.message; }
      btn.disabled = false;
    });

    setInterval(() => { if (!document.hidden && state.tab !== 'submit' && store.get() && !$('#drawer').classList.contains('open')) load(); }, 30000);
    applyAdminUi();
    if (store.get()) showTab('leads');
  }
  init();
})();
