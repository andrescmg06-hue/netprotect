import { GoogleSignInButton } from "@/components/GoogleSignInButton";
import { Logo, Spinner } from "@/components/ui";

import styles from "./LoginPanel.module.css";

/** The right-hand sign-in card. Google's own rendered button (GoogleSignInButton) keeps its
 * official look — reskinning an iframe Google controls is neither possible nor appropriate. */
export function LoginPanel({ authStatus }: { authStatus: "loading" | "unauthenticated" }) {
  return (
    <aside className={styles.panel}>
      <div className={styles.card}>
        <Logo height={48} />
        <h2 className={styles.title}>Bienvenido</h2>
        <p className={styles.lead}>Inicia sesión con tu cuenta de Google para acceder al panel del tutor.</p>

        <div className={styles.action} aria-live="polite">
          {authStatus === "loading" ? (
            <Spinner label="Comprobando sesión…" />
          ) : (
            <GoogleSignInButton />
          )}
        </div>

        <p className={styles.footnote}>Tu cuenta de Google es tu cuenta en NetProtect.</p>
      </div>
    </aside>
  );
}
