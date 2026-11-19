/* ==========================================================================
   data-layer.js — persistent cache-first data layer with stale-while-revalidate (SWR)

   Layer 2 (this file): IndexedDB copy of the last good public API responses.
   Layer 1 (server)   : api/[...path].js keeps a shared snapshot; /cache/*.json are build-time seeds.
   Layer 3 (this file): one central, de-duplicated, time-limited revalidation with exponential back-off.

   How it plugs in without touching every loader: window.fetch is wrapped for PUBLIC GET /api/* requests only.
   A loader that calls fetch('/api/profile') gets the cached JSON instantly; the real request runs in the
   background and, when newer data arrives, the loader registered for that URL is simply called again.
   ========================================================================== */
(function () {
  'use strict';

  var CFG = Object.assign({
    version: 1,                    // bump to invalidate every browser cache
    dbName: 'raviranjan-os-cache',
    storeName: 'entries',
    timeoutMs: 12000,              // per request
    freshMs: 60 * 1000,            // no background request while an entry is younger than this
    maxAgeMs: 90 * 24 * 3600 * 1000,
    backoffBaseMs: 4000,
    backoffMaxMs: 60000,
    maxRetries: 5,                 // per key, per "cycle" (reset by tab focus / coming back online)
    seedTimeoutMs: 3000,
    emptyAcceptAfter: 3,           // a live backend must report "empty" this many times ...
    emptyConfirmMs: 15 * 60 * 1000 // ... over at least this long before the cache accepts it
  }, window.__DATA_LAYER_CONFIG || {});

  var nativeFetch = window.fetch.bind(window);

  /* first-paint skeletons: empty hero fields shimmer (instead of staying blank) until any data has arrived */
  var SKELETON_IDS = ['hero-profile-name', 'overview-profile-name', 'overview-profile-headline', 'overview-profile-bio', 'hero-profile-bio', 'hero-curl-command', 'availability-card-title'];
  document.documentElement.setAttribute('data-dl-loading', '1');
  setTimeout(function () { document.documentElement.removeAttribute('data-dl-loading'); }, 8000);
  document.addEventListener('DOMContentLoaded', function () {
    SKELETON_IDS.forEach(function (id) { var el = document.getElementById(id); if (el) el.classList.add('skel'); });
  });

  /* ---- which requests are cacheable (public, GET, same origin) ---- */
  var PUBLIC_PATHS = [
    /^\/api\/profile$/, /^\/api\/settings$/, /^\/api\/projects(\/[A-Za-z0-9._-]+)?$/, /^\/api\/skills$/,
    /^\/api\/experiences$/, /^\/api\/education$/, /^\/api\/achievements$/, /^\/api\/certifications$/,
    /^\/api\/activity$/, /^\/api\/activity\/telemetry$/, /^\/api\/dsa\/problems$/, /^\/api\/research$/,
    /^\/api\/lab$/, /^\/api\/articles$/, /^\/api\/stories$/
  ];
  var CORE_KEYS = ['/api/profile', '/api/projects'];     // drive the "Live / Cached" indicator

  function keyFor(input, init) {
    try {
      var method = ((init && init.method) || (input && input.method) || 'GET').toUpperCase();
      if (method !== 'GET') return null;
      var raw = typeof input === 'string' ? input : (input && input.url) || String(input);
      var u = new URL(raw, window.location.origin);
      if (u.origin !== window.location.origin) return null;
      var h = (init && init.headers) || (input && input.headers) || {};
      var get = function (n) { return h && typeof h.get === 'function' ? h.get(n) : (h && (h[n] || h[n.toLowerCase()])); };
      if (get('Authorization') || get('X-Admin-Token')) return null;                 // never cache private calls
      if (String(get('Accept') || '').indexOf('text/event-stream') !== -1) return null;
      if (init && (init.cache === 'no-store' || init.cache === 'reload')) return null;
      if (!PUBLIC_PATHS.some(function (re) { return re.test(u.pathname); })) return null;
      var params = Array.from(u.searchParams.entries()).sort(function (a, b) { return a[0] < b[0] ? -1 : 1; });
      return u.pathname + (params.length ? '?' + params.map(function (p) { return p[0] + '=' + p[1]; }).join('&') : '');
    } catch (e) { return null; }
  }

  /* ---- response validation: never let junk replace good data ---- */
  function hasContent(d) {
    if (d === null || d === undefined || d === '') return false;
    if (Array.isArray(d)) return d.length > 0;
    if (typeof d === 'object') {
      if (Array.isArray(d.content)) return d.content.length > 0;
      return Object.keys(d).length > 0;
    }
    return true;
  }
  function isStructurallyValid(json) { return !!json && typeof json === 'object' && json.success !== false && 'data' in json; }
  function isUsable(json) { return isStructurallyValid(json) && hasContent(json.data); }

  /* ---- IndexedDB (with an in-memory fallback when it is unavailable, e.g. private mode) ---- */
  var memory = new Map(), dbPromise = null;
  function openDb() {
    if (dbPromise) return dbPromise;
    dbPromise = new Promise(function (resolve) {
      if (!window.indexedDB) return resolve(null);
      try {
        var req = indexedDB.open(CFG.dbName, 1);
        req.onupgradeneeded = function () { req.result.createObjectStore(CFG.storeName, { keyPath: 'key' }); };
        req.onsuccess = function () { resolve(req.result); };
        req.onerror = function () { resolve(null); };
        req.onblocked = function () { resolve(null); };
      } catch (e) { resolve(null); }
    });
    return dbPromise;
  }
  function idb(mode, fn) {
    return openDb().then(function (db) {
      if (!db) return undefined;
      return new Promise(function (resolve) {
        try {
          var tx = db.transaction(CFG.storeName, mode), req = fn(tx.objectStore(CFG.storeName));
          req.onsuccess = function () { resolve(req.result); };
          req.onerror = function () { resolve(undefined); };
        } catch (e) { resolve(undefined); }
      });
    });
  }
  function dbGet(key) {
    return idb('readonly', function (s) { return s.get(key); }).then(function (r) { return r === undefined ? (memory.get(key) || null) : (r || null); });
  }
  function dbSet(entry) {
    memory.set(entry.key, entry);
    return idb('readwrite', function (s) { return s.put(entry); });
  }
  function dbDelete(key) {
    memory.delete(key);
    return idb('readwrite', function (s) { return s.delete(key); });
  }
  function dbClear() {
    memory.clear();
    return idb('readwrite', function (s) { return s.clear(); });
  }
  function validEntry(e) {
    if (!e || e.version !== CFG.version || e.data === undefined || e.data === null) return false;
    var t = Date.parse(e.updatedAt);
    if (isNaN(t) || Date.now() - t > CFG.maxAgeMs || t - Date.now() > 24 * 3600 * 1000) return false;   // expired or from the future
    return isStructurallyValid(e.data);
  }
  function hashOf(json) {
    var s = JSON.stringify(json), h = 5381;
    for (var i = 0; i < s.length; i++) h = ((h << 5) + h + s.charCodeAt(i)) | 0;
    return String(h) + ':' + s.length;
  }

  /* ---- state, listeners, status indicator ---- */
  var inflight = new Map(), retries = new Map(), retryTimers = new Map(), known = new Map(), loaders = [];
  var state = { pending: 0, liveAt: 0, updatedAt: null, hadData: false, coreReady: false, lastOutcome: {} };

  function relTime(iso) {
    var t = Date.parse(iso); if (isNaN(t)) return '';
    var s = Math.max(0, Math.round((Date.now() - t) / 1000));
    if (s < 45) return 'just now';
    if (s < 3600) return Math.round(s / 60) + ' min ago';
    if (s < 86400) return Math.round(s / 3600) + ' h ago';
    return Math.round(s / 86400) + ' d ago';
  }
  function overall() {
    var failing = Object.keys(state.lastOutcome).some(function (k) { return state.lastOutcome[k] === 'cached'; });
    // "Updating" only while nothing has failed yet; once the backend proved unreachable the indicator stays on
    // "Showing recently updated data" instead of flapping on every background retry.
    if (state.pending > 0 && state.hadData && !failing) return 'updating';
    var coreOk = CORE_KEYS.every(function (k) {
      var hit = Object.keys(state.lastOutcome).filter(function (x) { return x.indexOf(k) === 0; });
      return hit.length === 0 || hit.every(function (x) { return state.lastOutcome[x] === 'live'; });
    });
    return state.liveAt && coreOk ? 'live' : 'cached';
  }
  function paintStatus() {
    var st = overall();
    var label = st === 'updating' ? 'Updating\u2026' : st === 'live' ? 'Live' : 'Showing recently updated data';
    var time = state.updatedAt ? relTime(state.updatedAt) : '';
    var full = state.updatedAt ? 'Last updated: ' + new Date(state.updatedAt).toLocaleString() : '';
    [].forEach.call(document.querySelectorAll('[data-status-pill]'), function (el) {
      el.hidden = !state.hadData;
      el.setAttribute('data-state', st);
      if (full) el.title = full;
      var t = el.querySelector('[data-status-text]'), m = el.querySelector('[data-status-time]');
      if (t) t.textContent = label;
      if (m) m.textContent = st === 'updating' || !time ? '' : (st === 'live' ? '\u00b7 ' + time : '\u00b7 ' + time);
    });
    document.documentElement.setAttribute('data-dl-status', st);
    if (state.coreReady) document.documentElement.removeAttribute('data-dl-loading');   // skeletons stay until the profile itself is known
  }
  function noteUpdatedAt(iso) {
    if (!iso) return;
    if (!state.updatedAt || Date.parse(iso) > Date.parse(state.updatedAt)) state.updatedAt = iso;
  }

  var pendingLoaders = null;
  function notify(key) {
    loaders.forEach(function (l) { if (l.match(key)) (pendingLoaders = pendingLoaders || new Set()).add(l.fn); });
    if (!pendingLoaders) return;
    setTimeout(function () {
      var fns = Array.from(pendingLoaders || []); pendingLoaders = null;
      fns.forEach(function (fn) { try { fn(); } catch (e) { console.warn('[data-layer] loader failed', e); } });
    }, 30);
  }

  function synth(entry, source) {
    return new Response(JSON.stringify(entry.data), {
      status: 200,
      headers: { 'Content-Type': 'application/json', 'X-Data-Source': source || 'browser-cache', 'X-Data-Updated-At': entry.updatedAt }
    });
  }

  /* ---- seeds: static JSON written at build time (served by the CDN, so they are instant) ---- */
  function seedName(key) { return key.replace(/^\/api\//, '').replace(/[^a-z0-9]+/gi, '_').replace(/^_+|_+$/g, '') + '.json'; }
  function loadSeed(key) {
    var ctrl = new AbortController(), t = setTimeout(function () { ctrl.abort(); }, CFG.seedTimeoutMs);
    return nativeFetch('/cache/' + seedName(key), { signal: ctrl.signal }).then(function (r) {
      clearTimeout(t);
      if (!r.ok) return null;
      return r.json().then(function (j) {
        var e = { key: key, version: CFG.version, updatedAt: j && j.updatedAt, fetchedAt: 0, source: 'seed', data: j && j.data };
        return j && j.version === CFG.version && validEntry(e) && isUsable(e.data) ? Object.assign(e, { hash: hashOf(e.data) }) : null;
      });
    }).catch(function () { clearTimeout(t); return null; });
  }

  /* ---- the revalidation (one per key at a time, time-limited, validated) ---- */
  function requestNetwork(key, url, init) {
    if (inflight.has(key)) return inflight.get(key);
    state.pending++; paintStatus();
    var p = (async function () {
      var ctrl = new AbortController(), timer = setTimeout(function () { ctrl.abort(); }, CFG.timeoutMs);
      try {
        var opts = Object.assign({}, init || {}, { signal: ctrl.signal, method: 'GET' });
        var res = await nativeFetch(url, opts);
        if (!res.ok) return { ok: false, status: res.status };
        var json = await res.json();
        return {
          ok: true, json: json, structural: isStructurallyValid(json), usable: isUsable(json),
          source: res.headers.get('x-data-source') || 'live',
          updatedAt: res.headers.get('x-data-updated-at') || new Date().toISOString()
        };
      } catch (e) {
        return { ok: false, error: e };
      } finally { clearTimeout(timer); }
    })();
    inflight.set(key, p);
    var done = function () { inflight.delete(key); state.pending = Math.max(0, state.pending - 1); paintStatus(); };
    p.then(done, done);
    return p;
  }

  /** Runs a revalidation and merges the result into the cache. Resolves with the entry that is now current (or null). */
  async function revalidate(key, url, init, entry) {
    var r = await requestNetwork(key, url, init);
    var now = Date.now(), outcome;

    if (r.ok && r.usable) {
      var live = r.source === 'live';
      var newer = !entry || Date.parse(r.updatedAt) >= Date.parse(entry.updatedAt || 0);
      if (live || newer) {
        var hash = hashOf(r.json), changed = !entry || entry.hash !== hash;
        var fresh = { key: key, version: CFG.version, updatedAt: r.updatedAt, fetchedAt: now, source: r.source, hash: hash, emptyStreak: 0, data: r.json };
        if (changed || !entry || entry.fetchedAt === undefined) await dbSet(fresh); else await dbSet(Object.assign({}, entry, { fetchedAt: now, source: r.source, updatedAt: r.updatedAt }));
        noteUpdatedAt(r.updatedAt); state.hadData = true; if (key.indexOf('/api/profile') === 0) state.coreReady = true;
        if (changed) notify(key);
        entry = fresh;
      }
      outcome = live ? 'live' : 'cached';
      if (live) { state.liveAt = now; retries.delete(key); }
    } else if (r.ok && r.structural && entry && r.source === 'live') {
      // the live backend answered "empty": only believe it after several consecutive confirmations
      var streak = (entry.emptyStreak || 0) + 1, since = entry.emptySince || now;
      if (streak >= CFG.emptyAcceptAfter && now - since >= CFG.emptyConfirmMs) { await dbDelete(key); entry = null; notify(key); }
      else await dbSet(Object.assign({}, entry, { emptyStreak: streak, emptySince: since, fetchedAt: now }));
      outcome = 'live'; state.liveAt = now;
    } else {
      outcome = 'cached';                                   // timeout / error / junk: keep what we have
    }

    state.lastOutcome[key] = outcome;
    if (outcome !== 'live') scheduleRetry(key, url, init); else clearRetry(key);
    paintStatus();
    return entry;
  }

  function clearRetry(key) { var t = retryTimers.get(key); if (t) clearTimeout(t); retryTimers.delete(key); retries.delete(key); }
  function scheduleRetry(key, url, init) {
    if (retryTimers.has(key)) return;
    var n = retries.get(key) || 0;
    if (n >= CFG.maxRetries) return;                         // stop; tab focus / online will start a new cycle
    retries.set(key, n + 1);
    var delay = Math.min(CFG.backoffMaxMs, CFG.backoffBaseMs * Math.pow(2, n)) * (0.85 + Math.random() * 0.3);
    var timer = setTimeout(function () {
      retryTimers.delete(key);
      dbGet(key).then(function (e) { return revalidate(key, url, init, validEntry(e) ? e : null); });
    }, delay);
    retryTimers.set(key, timer);
  }

  function revalidateAllStale(force) {
    known.forEach(function (v, key) {
      dbGet(key).then(function (e) {
        var ok = validEntry(e);
        if (force || !ok || Date.now() - (e.fetchedAt || 0) > CFG.freshMs || state.lastOutcome[key] === 'cached') {
          clearRetry(key);
          revalidate(key, v.url, v.init, ok ? e : null);
        }
      });
    });
  }

  /* ---- the fetch wrapper ---- */
  async function cachedFetch(key, url, init) {
    known.set(key, { url: url, init: init && init.signal ? Object.assign({}, init, { signal: undefined }) : init });
    var entry = await dbGet(key);
    if (entry && !validEntry(entry)) { dbDelete(key); entry = null; }       // wrong version / corrupted / expired

    if (entry) {
      state.hadData = true; if (key.indexOf('/api/profile') === 0) state.coreReady = true; noteUpdatedAt(entry.updatedAt);
      if (Date.now() - (entry.fetchedAt || 0) > CFG.freshMs) revalidate(key, url, init, entry);
      else if (state.lastOutcome[key] === undefined) state.lastOutcome[key] = entry.source === 'live' ? 'live' : 'cached';
      paintStatus();
      return synth(entry, 'browser-cache');
    }

    var seed = await loadSeed(key);
    if (seed) {
      await dbSet(seed); state.hadData = true; if (key.indexOf('/api/profile') === 0) state.coreReady = true; noteUpdatedAt(seed.updatedAt); paintStatus();
      revalidate(key, url, init, seed);
      return synth(seed, 'seed');
    }

    var current = await revalidate(key, url, init, null);                  // nothing cached anywhere: wait once for the network
    if (current) return synth(current, current.source);
    return new Response(JSON.stringify({ success: false, message: 'Data is temporarily unavailable' }), { status: 503, headers: { 'Content-Type': 'application/json' } });
  }

  window.fetch = function (input, init) {
    var key = keyFor(input, init);
    if (!key) return nativeFetch(input, init);
    var url = typeof input === 'string' ? input : input.url;
    return cachedFetch(key, url, init);
  };

  /* ---- public API ---- */
  function matcher(pattern) {
    if (pattern instanceof RegExp) return function (k) { return pattern.test(k); };
    return function (k) { return k === pattern || k.indexOf(pattern + '?') === 0 || k.indexOf(pattern + '/') === 0; };
  }

  window.DataLayer = {
    config: CFG,
    /** Re-run `fn` whenever fresh data arrives for one of the given URL paths. */
    register: function (patterns, fn) {
      var ms = [].concat(patterns).map(matcher);
      loaders.push({ fn: fn, match: function (k) { return ms.some(function (m) { return m(k); }); } });
    },
    /** SWR for data that is not a same-origin API call (e.g. the public GitHub calendar). apply(json, meta) may run twice. */
    swrJson: async function (key, url, apply, opts) {
      opts = opts || {};
      var usable = opts.usable || isUsable;
      var entry = await dbGet(key);
      if (entry && !validEntry(entry)) { dbDelete(key); entry = null; }
      if (entry) { try { apply(entry.data, { source: 'browser-cache', updatedAt: entry.updatedAt }); } catch (e) {} }
      if (entry && Date.now() - (entry.fetchedAt || 0) < CFG.freshMs) return;
      var ctrl = new AbortController(), t = setTimeout(function () { ctrl.abort(); }, CFG.timeoutMs);
      try {
        var res = await nativeFetch(url, { signal: ctrl.signal });
        if (!res.ok) return;
        var json = await res.json();
        if (!usable(json)) return;
        var hash = hashOf(json), now = new Date().toISOString();
        await dbSet({ key: key, version: CFG.version, updatedAt: now, fetchedAt: Date.now(), source: 'live', hash: hash, data: { success: true, data: json } });
        if (!entry || entry.hash !== hash) { try { apply({ success: true, data: json }, { source: 'live', updatedAt: now }); } catch (e) {} }
      } catch (e) { /* keep the cached copy */ } finally { clearTimeout(t); }
    },
    status: function () { return { state: overall(), updatedAt: state.updatedAt, pending: state.pending }; },
    refresh: function () { revalidateAllStale(true); },
    clear: function () { return dbClear(); },
    _internals: { keyFor: keyFor, isUsable: isUsable, hashOf: hashOf, seedName: seedName }
  };

  /* ---- revalidate when the tab becomes active again or the network returns (no continuous polling) ---- */
  document.addEventListener('visibilitychange', function () {
    if (document.visibilityState !== 'visible') return;
    retries.clear();
    revalidateAllStale(false);
  });
  window.addEventListener('online', function () { retries.clear(); revalidateAllStale(true); });
  setInterval(paintStatus, 60 * 1000);                       // keeps "updated 3 min ago" honest
  document.addEventListener('DOMContentLoaded', paintStatus);
})();
