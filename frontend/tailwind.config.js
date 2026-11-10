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
        background: '#09090b',
        surface: '#18181b',
        'surface-elevated': '#171a23',
        border: '#27272a',
        'border-focus': '#3f3f46',
        primary: '#3b82f6',
        'primary-hover': '#2563eb',
        accent: '#10b981',
        warning: '#f59e0b',
        danger: '#ef4444',
        text: '#f4f4f5',
        'text-muted': '#a1a1aa',
        'text-subtle': '#71717a',
      },
      fontFamily: {
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
        sans: ['Inter', 'system-ui', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
