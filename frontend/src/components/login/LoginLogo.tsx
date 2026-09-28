import styles from "./LoginLogo.module.css";

/** Shield-with-family mark, shown once on this screen — inside the login card, horizontal (shield
 * left, "NetProtect" + "PANEL DEL TUTOR" stacked right). D7: an inline SVG (crisp at any size), not
 * the project's PNG `ui/Logo`, which keeps being used elsewhere in the panel. */
export function LoginLogo() {
  return (
    <div className={styles.logo}>
      <svg
        className={styles.shield}
        viewBox="0 0 86 100"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <defs>
          <linearGradient id="login-shield" x1="0" y1="0" x2="86" y2="100" gradientUnits="userSpaceOnUse">
            <stop stopColor="#0B3FBF" />
            <stop offset="1" stopColor="#3AA0FF" />
          </linearGradient>
        </defs>
        <path d="M43 0 84 15v27c0 30-17 48-41 58C19 90 2 72 2 42V15Z" fill="url(#login-shield)" />
        <circle cx="38" cy="42" r="9" fill="#FFFFFF" />
        <path d="M22 74c1-14 8-21 16-21s15 7 16 21c-10 6-22 6-32 0Z" fill="#FFFFFF" />
        <circle cx="56" cy="58" r="6.5" fill="#BFDBFE" />
        <path d="M45 79c1-10 6-15 11-15s10 5 11 15c-7 4-15 4-22 0Z" fill="#BFDBFE" />
      </svg>

      <div className={styles.wordmark}>
        <span className={styles.brand}>
          <span className={styles.brandNet}>Net</span>
          <span className={styles.brandProtect}>Protect</span>
        </span>
        <span className={styles.tagline}>Panel del tutor</span>
      </div>
    </div>
  );
}
