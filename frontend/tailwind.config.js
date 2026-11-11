/** Kept in sync with the inline Tailwind CDN config in public/*.html. All colours are theme tokens from public/design-system.css. */
module.exports = {
  darkMode: ['class'],
  content: ['./public/**/*.{html,js}'],
  theme: { extend: {
    fontFamily: { sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'], mono: ['JetBrains Mono', 'ui-monospace', 'monospace'] },
    colors: {
      canvas: 'rgb(var(--canvas) / <alpha-value>)', subtle: 'rgb(var(--subtle) / <alpha-value>)', card: 'rgb(var(--card) / <alpha-value>)',
      raised: 'rgb(var(--raised) / <alpha-value>)', line: 'rgb(var(--line) / <alpha-value>)', 'line-strong': 'rgb(var(--line-strong) / <alpha-value>)',
      fg: { DEFAULT: 'rgb(var(--fg) / <alpha-value>)', 2: 'rgb(var(--fg-2) / <alpha-value>)', 3: 'rgb(var(--fg-3) / <alpha-value>)' },
      brand: { DEFAULT: 'rgb(var(--brand) / <alpha-value>)', hover: 'rgb(var(--brand-hover) / <alpha-value>)', fg: 'rgb(var(--brand-fg) / <alpha-value>)' },
    },
    borderRadius: { sm: '6px', DEFAULT: '8px', md: '10px', lg: '12px', xl: '16px', '2xl': '20px', '3xl': '28px' },
    fontSize: { xs: ['0.8125rem', { lineHeight: '1.25rem' }] },
    borderColor: { DEFAULT: 'rgb(var(--line) / <alpha-value>)' },
  } },
  plugins: [],
};
