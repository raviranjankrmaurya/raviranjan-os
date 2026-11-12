/* design-system.js — theme switch, header scroll state, scroll-spy (nav + bottom nav), admin bottom-nav sync, a11y */
(function () {
  var root = document.documentElement;
  var isAdmin = !!document.getElementById('admin-sidebar');
  var KEY = isAdmin ? 'admin_theme' : 'theme';
  var mq = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

  function current() { return root.classList.contains('light') ? 'light' : 'dark'; }
  function syncSwitch() {
    var t = current();
    [].forEach.call(document.querySelectorAll('[data-theme-set]'), function (b) {
      b.setAttribute('aria-pressed', String(b.getAttribute('data-theme-set') === t));
    });
  }
  function setTheme(mode) {
    root.classList.remove('dark', 'light'); root.classList.add(mode);
    try { localStorage.setItem(KEY, mode); } catch (e) {}
    try { if (isAdmin && typeof updateAdminThemeIcon === 'function') updateAdminThemeIcon(mode); else if (typeof updateThemeIcon === 'function') updateThemeIcon(mode); } catch (e) {}
    syncSwitch();
  }
  window.setThemeMode = setTheme;
  new MutationObserver(syncSwitch).observe(root, { attributes: true, attributeFilter: ['class'] });

  // follow the OS only while the visitor has not chosen manually
  if (mq && mq.addEventListener) {
    mq.addEventListener('change', function (e) {
      try { if (localStorage.getItem(KEY)) return; } catch (x) {}
      root.classList.toggle('dark', e.matches); root.classList.toggle('light', !e.matches); syncSwitch();
    });
  }


  /* ======================================================================
     Confirm dialog — one "Are you sure?" window for every destructive action
     confirmDialog({title, message, confirmText, danger, requireText}) -> Promise<boolean>
     ====================================================================== */
  window.confirmDialog = function (opts) {
    opts = opts || {};
    return new Promise(function (resolve) {
      var prev = document.activeElement;
      var uid = 'cf' + Math.random().toString(36).slice(2, 8);
      var wrap = document.createElement('div');
      wrap.className = 'confirm-overlay';
      wrap.setAttribute('role', 'alertdialog'); wrap.setAttribute('aria-modal', 'true');
      wrap.setAttribute('aria-labelledby', uid + 't'); wrap.setAttribute('aria-describedby', uid + 'm');
      var danger = opts.danger !== false;
      var box = document.createElement('div'); box.className = 'confirm-box';
      var ic = document.createElement('div'); ic.className = 'confirm-icon' + (danger ? ' is-danger' : '');
      ic.innerHTML = '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3"/><path d="M12 9v4"/><path d="M12 17h.01"/></svg>';
      var h = document.createElement('h2'); h.id = uid + 't'; h.textContent = opts.title || 'Are you sure?';
      var p = document.createElement('p'); p.id = uid + 'm'; p.textContent = opts.message || 'This action cannot be undone.';
      box.appendChild(ic); box.appendChild(h); box.appendChild(p);
      var input = null;
      if (opts.requireText) {
        var lab = document.createElement('label'); lab.className = 'confirm-label'; lab.textContent = 'Type ' + opts.requireText + ' to confirm';
        input = document.createElement('input'); input.type = 'text'; input.autocomplete = 'off'; input.className = 'confirm-input';
        lab.appendChild(input); box.appendChild(lab);
      }
      var row = document.createElement('div'); row.className = 'confirm-actions';
      var cancel = document.createElement('button'); cancel.type = 'button'; cancel.className = 'btn btn--pill'; cancel.textContent = opts.cancelText || 'Cancel';
      var ok = document.createElement('button'); ok.type = 'button'; ok.className = 'btn btn--pill ' + (danger ? 'btn--danger' : 'btn--primary'); ok.textContent = opts.confirmText || 'Delete';
      if (input) { ok.disabled = true; input.addEventListener('input', function () { ok.disabled = input.value.trim() !== opts.requireText; }); }
      row.appendChild(cancel); row.appendChild(ok); box.appendChild(row); wrap.appendChild(box);
      document.body.appendChild(wrap);
      var done = false;
      function finish(v) {
        if (done) return; done = true;
        document.removeEventListener('keydown', onKey, true); wrap.remove();
        try { if (prev && prev.focus) prev.focus(); } catch (e) {}
        resolve(v);
      }
      function onKey(e) {
        if (e.key === 'Escape') { e.preventDefault(); e.stopPropagation(); finish(false); }
        else if (e.key === 'Tab') {
          var f = [].slice.call(wrap.querySelectorAll('button:not(:disabled), input')); if (!f.length) return;
          var i = f.indexOf(document.activeElement);
          if (e.shiftKey && (i <= 0)) { e.preventDefault(); f[f.length - 1].focus(); }
          else if (!e.shiftKey && (i === f.length - 1)) { e.preventDefault(); f[0].focus(); }
        }
      }
      document.addEventListener('keydown', onKey, true);
      wrap.addEventListener('mousedown', function (e) { if (e.target === wrap) finish(false); });
      cancel.addEventListener('click', function () { finish(false); });
      ok.addEventListener('click', function () { finish(true); });
      (input || cancel).focus();           // safest default: focus Cancel, never the destructive button
    });
  };

  // Gate every destructive admin handler behind confirmDialog (no per-function edits needed)
  var DESTRUCTIVE = {
    handleDeleteGfgData:        ['Remove GeeksforGeeks data?', 'The stored GfG stats will be deleted from the database.'],
    handleDeleteActivityItem:   ['Delete this activity item?'],
    handleDeleteDsaProblem:     ['Delete this DSA problem?'],
    handleDeleteProject:        ['Delete this project?', 'The project and its milestones, changelog and decisions are removed permanently.'],
    handleDeleteAllProjects:    ['Delete ALL projects?', 'Every project in the database will be removed permanently. This cannot be undone.', 'Delete all', 'DELETE ALL'],
    handleDeleteEducation:      ['Delete this education entry?'],
    handleDeleteExperience:     ['Delete this experience entry?'],
    handleDeleteAchievement:    ['Delete this achievement?'],
    handleDeleteCertification:  ['Delete this certification?'],
    handleDeleteResearch:       ['Delete this research item?'],
    handleDeleteLab:            ['Delete this lab item?'],
    handleDeleteStory:          ['Delete this contribution story?'],
    handleDeleteArticle:        ['Delete this article?'],
    handleDeleteInquiry:        ['Delete this inquiry?', 'The message and its moderation record are removed permanently.'],
    handleRetentionCleanup:     ['Apply privacy retention?', 'Old inquiry personal data will be anonymised or removed according to the retention window. This cannot be undone.', 'Apply'],
    handleDeleteProjectMilestone:  ['Delete this milestone?'],
    handleDeleteProjectChangelog:  ['Delete this changelog entry?'],
    handleDeleteProjectDecision:   ['Delete this decision record?'],
    resetHomepageLayout:        ['Reset homepage layout?', 'Your custom homepage layout will be replaced by the default one.', 'Reset'],
    deleteNotificationById:     ['Delete this notification?']
  };
  function gateDestructive() {
    Object.keys(DESTRUCTIVE).forEach(function (name) {
      var orig = window[name]; if (typeof orig !== 'function' || orig.__gated) return;
      var c = DESTRUCTIVE[name];
      var gated = function () {
        var self = this, args = arguments;
        return window.confirmDialog({
          title: c[0], message: c[1] || "This will be permanently deleted from the database. This can't be undone.",
          confirmText: c[2] || 'Delete', requireText: c[3] || ''
        }).then(function (yes) { if (yes) return orig.apply(self, args); });
      };
      gated.__gated = true; window[name] = gated;
    });
  }

  /* ======================================================================
     Esc / close / back — one stack for every overlay, drawer, modal and editor
     ====================================================================== */
  function visible(el) { return !!el && !el.classList.contains('hidden') && getComputedStyle(el).display !== 'none'; }
  function closeModalEl(m) {
    var b = m.querySelector('[onclick*="close"], [data-close], [aria-label="Close"], [aria-label="Close dialog"]');
    if (b) b.click(); else m.classList.add('hidden');
  }
  function closeTopLayer() {
    // overlay containers only: fixed, full-screen, with an id (never the login gate, backdrops or drawers)
    var EXCLUDE = { 'admin-direct-login-gate': 1, 'admin-sidebar-backdrop': 1, 'toast-container': 1, 'ai-drawer': 1, 'notification-drawer': 1, 'admin-sidebar': 1 };
    var modals = [].slice.call(document.querySelectorAll('[id].fixed.inset-0')).filter(function (m) { return !EXCLUDE[m.id] && visible(m); });
    if (modals.length) { closeModalEl(modals[modals.length - 1]); return true; }
    var d = document.getElementById('notification-drawer');
    if (d && !d.classList.contains('translate-x-full') && typeof window.toggleNotificationDrawer === 'function') { window.toggleNotificationDrawer(); return true; }
    var ai = document.getElementById('ai-drawer');
    if (ai && !ai.classList.contains('translate-x-full')) {
      var fn = window.toggleAiDrawer || window.togglePublicAiDrawer; if (fn) { fn(); return true; }
    }
    var sb = document.getElementById('admin-sidebar');
    if (sb && !sb.classList.contains('-translate-x-full') && window.innerWidth < 1024 && typeof window.toggleAdminMobileSidebar === 'function') { window.toggleAdminMobileSidebar(false); return true; }
    var pm = document.getElementById('mobile-nav-menu');
    if (pm && !pm.classList.contains('hidden')) { pm.classList.add('hidden'); return true; }
    var ed = document.getElementById('view-project-editor');
    if (visible(ed)) {
      var t = document.activeElement && document.activeElement.tagName;
      if (t === 'INPUT' || t === 'TEXTAREA' || t === 'SELECT') { document.activeElement.blur(); return true; }
      window.closeProjectEditor(); return true;
    }
    return false;
  }
  document.addEventListener('keydown', function (e) {
    if (e.key !== 'Escape' || e.defaultPrevented) return;
    if (document.querySelector('.confirm-overlay')) return;     // confirm dialog owns Esc
    if (closeTopLayer()) e.preventDefault();
  });

  // Project editor: Back / Close with an unsaved-changes guard
  var editorDirty = false;
  window.closeProjectEditor = function () {
    var go = function () { editorDirty = false; if (typeof window.switchView === 'function') window.switchView('projects'); };
    if (!editorDirty) return go();
    return window.confirmDialog({ title: 'Leave without saving?', message: 'You have unsaved changes in this project. They will be lost.', confirmText: 'Discard changes' }).then(function (y) { if (y) go(); });
  };


  /* ======================================================================
     AI streaming — real SSE when the backend offers /api/ai/chat/stream,
     otherwise the normal JSON endpoint revealed progressively.
     ====================================================================== */
  window.formatAiText = function (raw) {
    var esc = window.escapeHtml || function (t) { return String(t).replace(/[&<>"']/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]; }); };
    return esc(String(raw || ''))
      .replace(/\*\*(.+?)\*\*/g, '<strong class="text-fg">$1</strong>')
      .replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, function (m, label, href) {
        var clean = href.replace(/&amp;/g, '&');
        return /^(https?:\/\/|mailto:|#|\/)/i.test(clean)
          ? '<a href="' + href + '" target="_blank" rel="noopener noreferrer" class="text-indigo-400 hover:underline">' + label + '</a>' : label;
      })
      .replace(/\n/g, '<br>');
  };

  function aiStreamKnown() { try { return sessionStorage.getItem('ai_stream_ok'); } catch (e) { return null; } }
  function aiStreamRemember(v) { try { sessionStorage.setItem('ai_stream_ok', v ? '1' : '0'); } catch (e) {} }

  function sleep(ms, signal) {
    return new Promise(function (resolve, reject) {
      var t = setTimeout(resolve, ms);
      if (signal) signal.addEventListener('abort', function () { clearTimeout(t); reject(new DOMException('Aborted', 'AbortError')); }, { once: true });
    });
  }

  async function revealProgressively(text, opts) {
    var reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    var parts = text.match(/\S+\s*/g) || [];
    if (reduce || parts.length < 3) { opts.onText(text); return; }
    var perTick = Math.max(1, Math.ceil(parts.length / 80));          // cap the reveal at roughly 2 seconds
    var shown = '';
    for (var i = 0; i < parts.length; i += perTick) {
      shown += parts.slice(i, i + perTick).join('');
      opts.onText(shown);
      await sleep(26, opts.signal);
    }
    opts.onText(text);
  }

  async function readSse(res, opts) {
    var reader = res.body.getReader(), dec = new TextDecoder(), buf = '', text = '', meta = {}, finished = false;
    var flush = function () { opts.onText(text); };
    while (!finished) {
      var chunk = await reader.read();
      if (chunk.done) break;
      buf += dec.decode(chunk.value, { stream: true });
      var blocks = buf.split(/\r?\n\r?\n/); buf = blocks.pop();
      for (var b = 0; b < blocks.length; b++) {
        var ev = 'message', data = [];
        blocks[b].split(/\r?\n/).forEach(function (line) {
          if (line.indexOf('event:') === 0) ev = line.slice(6).trim();
          else if (line.indexOf('data:') === 0) data.push(line.slice(5).replace(/^ /, ''));
        });
        var payload = data.join('\n'); if (!payload) continue;
        if (ev === 'meta') { try { meta = JSON.parse(payload); opts.onMeta && opts.onMeta(meta); } catch (e) {} }
        else if (ev === 'error') { throw new Error(payload); }
        else if (ev === 'done') { finished = true; }
        else {
          var piece = payload;
          try { var j = JSON.parse(payload); piece = (typeof j === 'string') ? j : (j.text || j.token || j.delta || ''); } catch (e) {}
          text += piece; flush();
        }
      }
    }
    return { text: text, meta: meta };
  }

  window.streamAiReply = async function (opts) {
    var t0 = performance.now();
    var headers = Object.assign({ 'Content-Type': 'application/json' }, opts.headers || {});
    var body = JSON.stringify(opts.body || {});
    if (aiStreamKnown() !== '0') {
      try {
        var res = await fetch(opts.streamUrl || '/api/ai/chat/stream', { method: 'POST', headers: Object.assign({ Accept: 'text/event-stream' }, headers), body: body, signal: opts.signal });
        var ct = (res.headers.get('content-type') || '').toLowerCase();
        if (res.ok && ct.indexOf('text/event-stream') !== -1 && res.body) {
          aiStreamRemember(true);
          var out = await readSse(res, opts);
          return { text: out.text, meta: out.meta, ms: Math.round(performance.now() - t0), streamed: true };
        }
        aiStreamRemember(false);              // endpoint missing: don't probe again this session
      } catch (e) { if (e && e.name === 'AbortError') throw e; aiStreamRemember(false); }
    }
    var r = await fetch(opts.fallbackUrl || '/api/ai/chat', { method: 'POST', headers: headers, body: body, signal: opts.signal });
    if (!r.ok) throw new Error('AI service responded ' + r.status);
    var json = await r.json(); var d = (json && json.data) || {};
    if (opts.onMeta) opts.onMeta({ sessionId: d.sessionId, latencyMs: d.latencyMs, sources: d.sources });
    var answer = d.answer || '';
    await revealProgressively(answer, opts);
    return { text: answer, meta: d, ms: Math.round(performance.now() - t0), streamed: false };
  };

  /* ======================================================================
     Mobile: one section at a time. Home shows everything; every other
     nav item shows only that section.
     ====================================================================== */
  (function () {
    var main = document.getElementById('standard-view');
    if (!main || !window.matchMedia) return;
    var MQ = window.matchMedia('(max-width: 1023px)');
    var secs = [].slice.call(main.children).filter(function (el) { return el.tagName === 'SECTION' && el.id; });
    var ids = secs.map(function (s) { return s.id; });
    var current = '';
    window.__mobileFocusActive = function () { return MQ.matches; };

    function paintNav(id) {
      [].forEach.call(document.querySelectorAll('.bottom-nav--public [data-spy]'), function (a) {
        var on = (id === '' && a.getAttribute('data-spy') === 'overview') || a.getAttribute('data-spy') === id;
        if (on) a.setAttribute('aria-current', 'true'); else a.removeAttribute('aria-current');
      });
    }
    function show(id, push) {
      if (!MQ.matches) return false;
      id = (id === 'overview') ? '' : id;
      current = id;
      if (id) document.body.setAttribute('data-mobile-focus', id); else document.body.removeAttribute('data-mobile-focus');
      secs.forEach(function (s) { s.classList.toggle('is-focus', s.id === id); });
      paintNav(id);
      var menu = document.getElementById('mobile-nav-menu'); if (menu) menu.classList.add('hidden');
      if (push) { try { history.pushState({ mf: id }, '', id ? '#' + id : location.pathname + location.search); } catch (e) {} }
      window.scrollTo(0, 0);
      return true;
    }
    function reset() {
      document.body.removeAttribute('data-mobile-focus');
      secs.forEach(function (s) { s.classList.remove('is-focus'); });
      current = '';
    }

    document.addEventListener('click', function (e) {
      if (!MQ.matches || !e.target.closest) return;
      var brand = e.target.closest('.brand');
      if (brand && brand.closest('.site-header')) { show('', true); return; }
      var a = e.target.closest('a[href^="#"]'); if (!a) return;
      var id = a.getAttribute('href').slice(1);
      if (ids.indexOf(id) === -1) return;
      e.preventDefault(); show(id, true);
    }, true);

    var origNav = window.navigateSection;
    window.navigateSection = function (id) {
      if (MQ.matches) { if (typeof window.toggleCommandPalette === 'function') window.toggleCommandPalette(false); show(id, true); return; }
      return origNav ? origNav.apply(this, arguments) : undefined;
    };

    window.addEventListener('popstate', function (e) {
      if (!MQ.matches) return;
      var h = (location.hash || '').slice(1);
      show((e.state && typeof e.state.mf === 'string') ? e.state.mf : (ids.indexOf(h) !== -1 ? h : ''), false);
    });
    var onMq = function () {
      if (!MQ.matches) { reset(); return; }
      var h = (location.hash || '').slice(1);
      show(ids.indexOf(h) !== -1 ? h : '', false);
    };
    if (MQ.addEventListener) MQ.addEventListener('change', onMq); else if (MQ.addListener) MQ.addListener(onMq);
    document.addEventListener('DOMContentLoaded', function () {
      try { history.replaceState({ mf: current }, '', location.href); } catch (e) {}
      onMq();
    });
  })();

  // screen readers: announce streamed AI answers politely
  document.addEventListener('DOMContentLoaded', function () {
    ['ai-chat-messages', 'ai-messages-container'].forEach(function (id) {
      var el = document.getElementById(id); if (el) { el.setAttribute('role', 'log'); el.setAttribute('aria-live', 'polite'); }
    });
  });

  document.addEventListener('DOMContentLoaded', function () {

    gateDestructive();

    var ed = document.getElementById('view-project-editor');
    if (ed) {
      ['input', 'change'].forEach(function (ev) { ed.addEventListener(ev, function () { editorDirty = true; }); });
      if (typeof window.save13TabProject === 'function' && !window.save13TabProject.__wrapped) {
        var osave = window.save13TabProject;
        window.save13TabProject = function () {
          var r = osave.apply(this, arguments);
          Promise.resolve(r).then(function (v) { if (v !== false) editorDirty = false; });
          return r;
        };
        window.save13TabProject.__wrapped = true;
      }
    }

    // visible Close buttons where there were none
    var sbHead = document.querySelector('#admin-sidebar > div > div:first-child');
    if (sbHead && !sbHead.querySelector('[data-close-sidebar]')) {
      var x = document.createElement('button'); x.type = 'button'; x.setAttribute('data-close-sidebar', ''); x.setAttribute('aria-label', 'Close menu');
      x.className = 'btn btn--icon btn--pill lg:hidden ml-auto'; x.innerHTML = '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" aria-hidden="true"><path d="M18 6 6 18M6 6l12 12"/></svg>';
      x.addEventListener('click', function () { window.toggleAdminMobileSidebar(false); });
      sbHead.appendChild(x);
    }
    // Browser / phone Back button works between admin views
    if (isAdmin && typeof window.switchView === 'function') {
      var rawSwitch = window.switchView, fromPop = false;
      window.switchView = function (v) {
        var r = rawSwitch.apply(this, arguments);
        if (!fromPop && v && (history.state || {}).view !== v) { try { history.pushState({ view: v }, '', '#' + v); } catch (e) {} }
        return r;
      };
      window.addEventListener('popstate', function (e) {
        var v = (e.state && e.state.view) || (location.hash || '').slice(1) || 'dashboard';
        if (!document.getElementById('view-' + v)) return;
        fromPop = true; try { rawSwitch(v); } finally { fromPop = false; }
      });
      try { history.replaceState({ view: (location.hash || '#dashboard').slice(1) }, '', location.hash || '#dashboard'); } catch (e) {}
    }
    [].forEach.call(document.querySelectorAll('[data-theme-set]'), function (b) {
      b.addEventListener('click', function () { setTheme(b.getAttribute('data-theme-set')); });
    });
    syncSwitch();

    var header = document.querySelector('.site-header');
    if (header) {
      var onScroll = function () { header.classList.toggle('is-scrolled', window.scrollY > 4); };
      onScroll(); window.addEventListener('scroll', onScroll, { passive: true });
    }

    // Public: scroll-spy drives aria-current on the pill nav and the bottom nav
    var spyLinks = [].slice.call(document.querySelectorAll('.nav-link[href^="#"], .bottom-nav--public [data-spy]'));
    if ('IntersectionObserver' in window && spyLinks.length) {
      var map = {};
      spyLinks.forEach(function (a) { var id = a.getAttribute('href').slice(1); (map[id] = map[id] || []).push(a); });
      var io = new IntersectionObserver(function (entries) {
        entries.forEach(function (en) {
          if (!en.isIntersecting || (window.__mobileFocusActive && window.__mobileFocusActive())) return;
          spyLinks.forEach(function (a) { a.removeAttribute('aria-current'); });
          (map[en.target.id] || []).forEach(function (a) { a.setAttribute('aria-current', 'true'); });
        });
      }, { rootMargin: '-45% 0px -50% 0px' });
      Object.keys(map).forEach(function (id) { var el = document.getElementById(id); if (el) io.observe(el); });
    }

    // Public mobile menu: aria-expanded + Escape
    var btn = document.getElementById('mobile-nav-btn'), menu = document.getElementById('mobile-nav-menu');
    if (btn && menu) {
      var sync = function () { btn.setAttribute('aria-expanded', String(!menu.classList.contains('hidden'))); };
      new MutationObserver(sync).observe(menu, { attributes: true, attributeFilter: ['class'] }); sync();
      document.addEventListener('keydown', function (e) { if (e.key === 'Escape' && !menu.classList.contains('hidden')) { menu.classList.add('hidden'); btn.focus(); } });
    }

    // Admin: keep the bottom nav in step with the active view
    var adminNav = [].slice.call(document.querySelectorAll('.bottom-nav--admin [data-view]'));
    if (adminNav.length && typeof window.switchView === 'function') {
      var orig = window.switchView;
      var paint = function (v) { adminNav.forEach(function (b) { if (b.getAttribute('data-view') === v) b.setAttribute('aria-current', 'true'); else b.removeAttribute('aria-current'); }); };
      window.switchView = function (v) { var r = orig.apply(this, arguments); paint(v); return r; };
      paint('dashboard');
    }
  });
})();
