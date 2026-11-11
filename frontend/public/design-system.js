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

  document.addEventListener('DOMContentLoaded', function () {
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
          if (!en.isIntersecting) return;
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
