/** Only used by the /login-mockup route's own stylesheet (src/app/login-mockup/tailwind.css) —
 * every other route stays on plain CSS Modules + globals.css, unaffected by Tailwind's utility
 * generation. See docs/sprint-38-cierre.md for why this screen is the one exception. */
const config = {
  plugins: {
    "@tailwindcss/postcss": {},
  },
};

export default config;
