/* design-system.js — theme sync, header scroll state, scroll-spy, mobile menu a11y */
(function () {
  var root = document.documentElement;
  var mq = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

  // Follow the OS theme live, but only while the user has not chosen manually.
  if (mq && mq.addEventListener) {
    mq.addEventListener('change', function (e) {
      if (localStorage.getItem('theme')) return;
      root.classList.toggle('dark', e.matches);
      root.classList.toggle('light', !e.matches);
      if (typeof updateThemeIcon === 'function') updateThemeIcon(e.matches ? 'dark' : 'light');
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    var header = document.querySelector('.site-header');
    if (header) {
      var onScroll = function () { header.classList.toggle('is-scrolled', window.scrollY > 4); };
      onScroll();
      window.addEventListener('scroll', onScroll, { passive: true });
    }

    // Scroll-spy -> aria-current on desktop nav
    var links = [].slice.call(document.querySelectorAll('.nav-link[href^="#"]'));
    var map = {};
    links.forEach(function (a) { map[a.getAttribute('href').slice(1)] = a; });
    if ('IntersectionObserver' in window && links.length) {
      var io = new IntersectionObserver(function (entries) {
        entries.forEach(function (en) {
          if (!en.isIntersecting) return;
          links.forEach(function (a) { a.removeAttribute('aria-current'); });
          var a = map[en.target.id];
          if (a) a.setAttribute('aria-current', 'true');
        });
      }, { rootMargin: '-45% 0px -50% 0px' });
      Object.keys(map).forEach(function (id) { var el = document.getElementById(id); if (el) io.observe(el); });
    }

    // Mobile menu: reflect state in aria-expanded, close on Escape
    var btn = document.getElementById('mobile-nav-btn');
    var menu = document.getElementById('mobile-nav-menu');
    if (btn && menu) {
      var sync = function () { btn.setAttribute('aria-expanded', String(!menu.classList.contains('hidden'))); };
      new MutationObserver(sync).observe(menu, { attributes: true, attributeFilter: ['class'] });
      sync();
      document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && !menu.classList.contains('hidden')) { menu.classList.add('hidden'); btn.focus(); }
      });
    }
  });
})();
