/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        background: "#030305",   // absolute deep midnight
        surface: "#0A0A0F",      // slightly elevated panel base
        surfaceElevated: "#12121A", // higher elevation
        foreground: "#FFFFFF",   // crisp white
        muted: "#94A3B8",        // soft slate gray for secondary text
        primary: "#00E5FF",      // electric cyan
        primaryAccent: "#00B3FF",// intense blue
        secondary: "#B026FF",    // neon purple
        danger: "#FF2A55",       // vivid pink-red
        success: "#00FF94",      // neon green
        warning: "#FFD600"       // brilliant yellow
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'sans-serif'],
      },
      boxShadow: {
        'glow-cyan': '0 0 20px rgba(0, 229, 255, 0.4)',
        'glow-purple': '0 0 20px rgba(176, 38, 255, 0.4)',
        'glass-inset': 'inset 0 1px 0 0 rgba(255, 255, 255, 0.05)',
      }
    },
  },
  plugins: [],
}