/* ============================================================
   Raviranjan.OS — Tailwind Theme Bridge (v2)
   ------------------------------------------------------------
   The design system is token driven. Every Tailwind colour used
   by the product maps onto a semantic token declared in
   design-system.css, so light / dark mode, brand re-tinting and
   future palette changes happen in ONE place.

   Colour scales are declared as `rgb(var(--token) / <alpha-value>)`
   so utilities such as `bg-brand-500/10` keep working.
   ============================================================ */

(function () {
  const scale = (prefix) => ({
    50: `rgb(var(--c-${prefix}-50) / <alpha-value>)`,
    100: `rgb(var(--c-${prefix}-100) / <alpha-value>)`,
    200: `rgb(var(--c-${prefix}-200) / <alpha-value>)`,
    300: `rgb(var(--c-${prefix}-300) / <alpha-value>)`,
    400: `rgb(var(--c-${prefix}-400) / <alpha-value>)`,
    500: `rgb(var(--c-${prefix}-500) / <alpha-value>)`,
    600: `rgb(var(--c-${prefix}-600) / <alpha-value>)`,
    700: `rgb(var(--c-${prefix}-700) / <alpha-value>)`,
    800: `rgb(var(--c-${prefix}-800) / <alpha-value>)`,
    900: `rgb(var(--c-${prefix}-900) / <alpha-value>)`,
    950: `rgb(var(--c-${prefix}-950) / <alpha-value>)`,
  });

  const brand = scale('b');       // primary   — blue
  const success = scale('s');     // success   — green
  const warning = scale('w');     // warning   — amber
  const danger = scale('d');      // error     — red
  const accent = scale('a');      // accent    — violet
  const neutral = scale('n');     // neutrals  — zinc

  const semantic = {
    canvas: 'rgb(var(--c-canvas) / <alpha-value>)',
    surface: 'rgb(var(--c-surface) / <alpha-value>)',
    elevated: 'rgb(var(--c-elevated) / <alpha-value>)',
    raised: 'rgb(var(--c-hover) / <alpha-value>)',
    line: 'rgb(var(--c-line) / <alpha-value>)',
    'line-strong': 'rgb(var(--c-line-strong) / <alpha-value>)',
    ink: 'rgb(var(--c-ink) / <alpha-value>)',
    muted: 'rgb(var(--c-muted) / <alpha-value>)',
    faint: 'rgb(var(--c-faint) / <alpha-value>)',
    'on-solid': 'rgb(var(--c-on-solid) / <alpha-value>)',
    scrim: 'rgb(var(--c-scrim) / <alpha-value>)',
    knob: 'rgb(var(--c-on-solid) / <alpha-value>)',
  };

  window.tailwind = window.tailwind || {};
  window.tailwind.config = {
    darkMode: 'class',
    theme: {
      extend: {
        fontFamily: {
          sans: ['Inter', 'system-ui', '-apple-system', 'Segoe UI', 'sans-serif'],
          heading: ['Geist', 'Inter', 'system-ui', 'sans-serif'],
          mono: ['JetBrains Mono', 'ui-monospace', 'SFMono-Regular', 'monospace'],
        },
        colors: {
          ...semantic,
          brand,
          success,
          warning,
          danger,
          accent,
          neutral,
          /* --- legacy families re-pointed at the new system --- */
          sky: brand,
          cyan: brand,
          blue: brand,
          emerald: success,
          green: success,
          teal: success,
          amber: warning,
          yellow: warning,
          orange: warning,
          rose: danger,
          red: danger,
          pink: danger,
          purple: accent,
          violet: accent,
          indigo: accent,
          fuchsia: accent,
          slate: neutral,
          gray: neutral,
          zinc: neutral,
          /* text defaults: `text-white` becomes theme ink, `bg-black/70` the scrim */
          white: 'rgb(var(--c-ink) / <alpha-value>)',
          black: 'rgb(var(--c-scrim) / <alpha-value>)',
        },
        borderRadius: {
          none: '0px',
          sm: '4px',
          DEFAULT: '6px',
          md: '8px',
          lg: '10px',
          xl: '12px',
          '2xl': '16px',
          '3xl': '20px',
          full: '999px',
        },
        boxShadow: {
          sm: 'var(--ds-shadow-sm)',
          DEFAULT: 'var(--ds-shadow)',
          md: 'var(--ds-shadow-md)',
          lg: 'var(--ds-shadow-lg)',
          xl: 'var(--ds-shadow-xl)',
          '2xl': 'var(--ds-shadow-xl)',
          inner: 'var(--ds-shadow-inner)',
          none: 'none',
        },
        transitionTimingFunction: {
          out: 'cubic-bezier(0.16, 1, 0.3, 1)',
          in: 'cubic-bezier(0.4, 0, 1, 1)',
          'in-out': 'cubic-bezier(0.4, 0, 0.2, 1)',
        },
        transitionDuration: {
          fast: '140ms',
          DEFAULT: '180ms',
          slow: '260ms',
        },
        maxWidth: {
          content: '1200px',
        },
        screens: {
          xs: '375px',
        },
      },
    },
  };
})();
