import styles from "./LoginFooter.module.css";

const LINKS = [
  { label: "Ayuda", href: "#" },
  { label: "Privacidad", href: "#" },
  { label: "Términos", href: "#" },
];

/** Real placeholder links (`href="#"`), not yet wired to pages that don't exist — matches the brief
 * exactly (D3: kept as-is by decision). Brand label on the left, links on the right, no underline
 * until hover/focus. The links keep `color: var(--color-primary-text)` from globals.css `a`. */
export function LoginFooter() {
  return (
    <footer className={styles.footer}>
      <span>
        <span className={styles.brand}>NetProtect</span>
        <span className={styles.sep}> · Panel del tutor</span>
      </span>

      <nav aria-label="Enlaces del pie de página" className={styles.nav}>
        {LINKS.map((link) => (
          <a key={link.label} href={link.href} className={styles.link}>
            {link.label}
          </a>
        ))}
      </nav>
    </footer>
  );
}
