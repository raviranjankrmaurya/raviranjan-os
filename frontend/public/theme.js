/* ============================================================
   Raviranjan.OS — Theme Engine (v2)
   ------------------------------------------------------------
   · Follows the OS preference by default ("system")
   · Manual override persists per surface (site / admin)
   · Applies the class before first paint (see inline bootstrap
     in <head>) so there is never a flash of the wrong theme
   · Keeps <meta name="theme-color"> in sync for mobile chrome
   ============================================================ */

(function () {
  const STORAGE = {
    site: 'theme',          // legacy key kept for compatibility
    admin: 'admin_theme',
  };
  const media = window.matchMedia('(prefers-color-scheme: light)');

  function keyFor(surface) {
    return STORAGE[surface] || STORAGE.site;
  }

  function storedMode(surface) {
    try {
      const raw = localStorage.getItem(keyFor(surface));
      if (raw === 'light' || raw === 'dark' || raw === 'system') return raw;
    } catch (e) { /* private mode */ }
    return 'system';
  }

  function resolve(mode) {
    if (mode === 'light' || mode === 'dark') return mode;
    return media.matches ? 'light' : 'dark';
  }

  function paint(theme) {
    const root = document.documentElement;
    root.classList.toggle('light', theme === 'light');
    root.classList.toggle('dark', theme === 'dark');
    root.style.colorScheme = theme;
    root.setAttribute('data-theme', theme);
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', theme === 'light' ? '#FFFFFF' : '#09090B');
  }

  function emit(theme, mode, surface) {
    window.dispatchEvent(new CustomEvent('themechange', {
      detail: { theme, mode, surface },
    }));
    document.dispatchEvent(new CustomEvent('ds:themechange', {
      detail: { theme, mode, surface },
    }));
  }

  function apply(mode, surface = 'site', persist = true) {
    const theme = resolve(mode);
    paint(theme);
    if (persist) {
      try { localStorage.setItem(keyFor(surface), mode); } catch (e) { /* ignore */ }
    }
    emit(theme, mode, surface);
    return theme;
  }

  const DSTheme = {
    /** Current resolved theme: 'light' | 'dark' */
    get theme() {
      return document.documentElement.classList.contains('light') ? 'light' : 'dark';
    },
    /** Current preference: 'system' | 'light' | 'dark' */
    mode(surface = 'site') {
      return storedMode(surface);
    },
    /** Apply the persisted preference (call once on boot). */
    init(surface = 'site') {
      return apply(storedMode(surface), surface, false);
    },
    set(mode, surface = 'site') {
      return apply(mode, surface, true);
    },
    /** Flip between explicit light and dark. */
    toggle(surface = 'site') {
      return apply(this.theme === 'dark' ? 'light' : 'dark', surface, true);
    },
    /** Cycle system → light → dark → system. */
    cycle(surface = 'site') {
      const order = ['system', 'light', 'dark'];
      const next = order[(order.indexOf(storedMode(surface)) + 1) % order.length];
      return apply(next, surface, true);
    },
    /** Re-apply when the OS preference changes while in "system" mode. */
    watch(surface = 'site') {
      const onChange = () => {
        if (storedMode(surface) === 'system') apply('system', surface, false);
      };
      if (media.addEventListener) media.addEventListener('change', onChange);
      else if (media.addListener) media.addListener(onChange);
    },
  };

  /* ------------------------------------------------------------------
     Brand re-tint. A single accent colour re-points the brand scale in
     both themes, with lightness tuned so text keeps AA contrast.
     ------------------------------------------------------------------ */
  function hexToHsl(hex) {
    const v = String(hex).replace('#', '');
    const r = parseInt(v.substring(0, 2), 16) / 255;
    const g = parseInt(v.substring(2, 4), 16) / 255;
    const b = parseInt(v.substring(4, 6), 16) / 255;
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    const l = (max + min) / 2;
    let h = 0;
    let sat = 0;
    if (max !== min) {
      const d = max - min;
      sat = l > 0.5 ? d / (2 - max - min) : d / (max + min);
      if (max === r) h = ((g - b) / d + (g < b ? 6 : 0)) / 6;
      else if (max === g) h = ((b - r) / d + 2) / 6;
      else h = ((r - g) / d + 4) / 6;
    }
    return { h: h * 360, s: sat * 100, l: l * 100 };
  }

  function hslChannels(h, s, l) {
    const sat = Math.max(0, Math.min(100, s)) / 100;
    const lig = Math.max(0, Math.min(100, l)) / 100;
    const c = (1 - Math.abs(2 * lig - 1)) * sat;
    const hp = (((h % 360) + 360) % 360) / 60;
    const x = c * (1 - Math.abs((hp % 2) - 1));
    let rgb = [0, 0, 0];
    if (hp < 1) rgb = [c, x, 0];
    else if (hp < 2) rgb = [x, c, 0];
    else if (hp < 3) rgb = [0, c, x];
    else if (hp < 4) rgb = [0, x, c];
    else if (hp < 5) rgb = [x, 0, c];
    else rgb = [c, 0, x];
    const m = lig - c / 2;
    return rgb.map((v) => Math.round((v + m) * 255)).join(' ');
  }

  DSTheme.applyBrand = function (hex) {
    if (!/^#[0-9a-fA-F]{6}$/.test(String(hex || ''))) return;
    try { localStorage.setItem('raviranjan_accent_color', hex); } catch (e) {}
    const { h, s, l } = hexToHsl(hex);
    const dark = {
      300: hslChannels(h, s, Math.min(88, l + 26)),
      400: hslChannels(h, s, Math.min(80, l + 13)),
      500: hslChannels(h, s, l),
      600: hslChannels(h, s, Math.max(22, l - 9)),
      700: hslChannels(h, s, Math.max(18, l - 17)),
      800: hslChannels(h, s, Math.max(15, l - 25)),
      950: hslChannels(h, s, Math.max(12, l - 34)),
    };
    const light = {
      300: hslChannels(h, s, Math.max(18, Math.min(34, l - 22))),
      400: hslChannels(h, s, Math.max(22, Math.min(38, l - 14))),
      500: hslChannels(h, s, Math.max(24, Math.min(42, l - 8))),
      600: hslChannels(h, s, Math.max(20, Math.min(36, l - 16))),
      700: hslChannels(h, s, Math.max(18, Math.min(32, l - 22))),
      800: hslChannels(h, s, Math.max(16, Math.min(28, l - 28))),
      950: hslChannels(h, s, Math.max(12, Math.min(24, l - 34))),
    };
    const block = (selector, shades) =>
      selector + ' {\n' +
      Object.keys(shades).map((k) => '  --c-b-' + k + ': ' + shades[k] + ';').join('\n') +
      '\n}';
    let el = document.getElementById('ds-brand-tokens');
    if (!el) {
      el = document.createElement('style');
      el.id = 'ds-brand-tokens';
      document.head.appendChild(el);
    }
    el.textContent = block('html.dark', dark) + '\n' + block('html.light', light);
  };


  /* ------------------------------------------------------------------
     API transport — same-origin proxy, with a direct-backend fallback.
     ------------------------------------------------------------------
     Every app call uses a relative "/api/..." URL, so it only works when
     the page is served by something that proxies /api (server.js, Vercel
     rewrites, the Spring Boot app). On a plain static host that path
     returns 404 or the HTML shell, and the UI shows "loading" forever.

     This wrapper keeps the relative path as the first choice and, the
     first time it clearly is not being proxied, switches the whole session
     to the backend origin directly. The backend allows all origins
     (allowedOriginPatterns "*"), so this works from any host.

     Override the backend origin with:
       window.RAVIRANJAN_API_BASE = "https://your-backend"
     or <meta name="api-base" content="https://your-backend">
     ------------------------------------------------------------------ */
  (function installApiTransport() {
    if (window.__dsApiTransportInstalled) return;
    window.__dsApiTransportInstalled = true;

    var meta = document.querySelector('meta[name="api-base"]');
    var FALLBACK = String(
      window.RAVIRANJAN_API_BASE || (meta && meta.content) || 'https://raviranjan-os-backend.onrender.com'
    ).replace(/\/+$/, '');

    var nativeFetch = window.fetch ? window.fetch.bind(window) : null;
    if (!nativeFetch || !FALLBACK) return;

    var useDirect = false;      // resolved decision
    var probe = null;           // shared one-shot probe promise
    var probing = false;

    function isRelativeApi(url) {
      return typeof url === 'string' && (url === '/api' || url.indexOf('/api/') === 0 || url.indexOf('/api?') === 0);
    }

    function directUrl(url) {
      return FALLBACK + url;
    }

    // A proxy miss looks like: 404 / 405, or any 2xx that is HTML (the SPA shell
    // being served for /api/...). A 5xx means the proxy exists but the backend is
    // unhappy, so we stay on the same origin and let the UI show its error state.
    function looksUnproxied(res) {
      if (!res) return true;
      var type = (res.headers && res.headers.get && (res.headers.get('content-type') || '')) || '';
      if (res.status === 404 || res.status === 405) return true;
      if (res.ok && type.indexOf('text/html') === 0) return true;
      return false;
    }

    // One shared probe so a burst of parallel calls makes a single decision
    // instead of every request discovering the same thing.
    function resolveMode() {
      if (useDirect || probe) return probe || Promise.resolve(useDirect);
      probe = nativeFetch('/api', { cache: 'no-store' })
        .then(function (res) {
          if (looksUnproxied(res)) {
            useDirect = true;
            console.info('[Raviranjan.OS] /api is not proxied on this host — calling ' + FALLBACK + ' directly.');
          }
          return useDirect;
        })
        .catch(function () {
          useDirect = true;
          console.info('[Raviranjan.OS] /api unreachable on this host — calling ' + FALLBACK + ' directly.');
          return true;
        })
        .then(function (direct) {
          probing = false;
          return direct;
        });
      return probe;
    }

    window.fetch = function (input, init) {
      var url = typeof input === 'string' ? input : (input && input.url) || '';

      if (!isRelativeApi(url)) return nativeFetch(input, init);

      if (useDirect) return nativeFetch(directUrl(url), init);

      // While the very first decision is being made, queue behind it.
      if (probing) {
        return resolveMode().then(function (direct) {
          return nativeFetch(direct ? directUrl(url) : input, init);
        });
      }

      if (probe) {
        return probe.then(function (direct) {
          return nativeFetch(direct ? directUrl(url) : input, init);
        });
      }

      probing = true;
      return resolveMode().then(function (direct) {
        return nativeFetch(direct ? directUrl(url) : input, init);
      });
    };

    window.DS_API_BASE = FALLBACK;
    window.DS_API_IS_DIRECT = function () {
      return useDirect;
    };
  })();

  window.DSTheme = DSTheme;
})();
