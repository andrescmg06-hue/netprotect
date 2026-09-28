import { Spinner } from "@/components/ui";

import { GoogleButton } from "./GoogleButton";
import { LoginLogo } from "./LoginLogo";

import styles from "./LoginCard.module.css";

/** The sign-in card — the only place this screen shows the logo. Only Google sign-in, per the brief.
 *
 * D4: while `authStatus === "loading"`, the Google button is replaced by a "Comprobando sesión…"
 * spinner inside an `aria-live` container with the same height as the button, so the card doesn't
 * jump before the session check resolves. */
export function LoginCard({ authStatus }: { authStatus: "loading" | "unauthenticated" }) {
  return (
    <div className={styles.card}>
      <LoginLogo />

      <h1 className={styles.title}>Inicia sesión</h1>

      <p className={styles.subtitle}>
        Accede a tu panel para administrar tus dispositivos, aplicaciones y reglas.
      </p>

      <div className={styles.action}>
        {authStatus === "loading" ? (
          <div className={styles.loading} aria-live="polite">
            <Spinner label="Comprobando sesión…" />
          </div>
        ) : (
          <GoogleButton />
        )}
      </div>

      <div className={styles.footnote}>
        <p className={styles.footnoteText}>Tu cuenta de Google es tu cuenta en NetProtect.</p>
      </div>
    </div>
  );
}
