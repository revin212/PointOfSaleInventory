/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ["class"],
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        surface: "hsl(var(--surface))",
        "surface-container-low": "hsl(var(--surface-container-low))",
        "surface-container-lowest": "hsl(var(--surface-container-lowest))",
        "surface-container-highest": "hsl(var(--surface-container-highest))",
        primary: "hsl(var(--primary))",
        "primary-container": "hsl(var(--primary-container))",
        "on-surface": "hsl(var(--on-surface))",
        "on-surface-variant": "hsl(var(--on-surface-variant))",
        error: "hsl(var(--error))",
        "outline-variant": "hsl(var(--outline-variant))",
      },
      borderRadius: {
        lg: "var(--radius)",
        xl: "calc(var(--radius) + 4px)",
      },
      fontFamily: {
        sans: ["Inter", "sans-serif"],
      },
      boxShadow: {
        ambient: "0 20px 40px -10px rgba(11, 28, 48, 0.06)",
      },
    },
  },
  plugins: [],
}

