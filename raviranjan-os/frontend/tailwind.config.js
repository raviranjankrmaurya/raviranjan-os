/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: ["class"],
  content: [
    './pages/**/*.{ts,tsx}',
    './components/**/*.{ts,tsx}',
    './app/**/*.{ts,tsx}',
    './src/**/*.{ts,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        background: '#090a0f',
        surface: '#11131a',
        'surface-elevated': '#171a23',
        border: '#232733',
        'border-focus': '#3b4255',
        primary: '#38bdf8',
        'primary-hover': '#0284c7',
        accent: '#10b981',
        warning: '#f59e0b',
        danger: '#ef4444',
        text: '#f1f5f9',
        'text-muted': '#94a3b8',
        'text-subtle': '#64748b',
      },
      fontFamily: {
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
        sans: ['Inter', 'system-ui', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
