import { BackgroundScene } from "./BackgroundScene";
import { HeroShowcase } from "./HeroShowcase";
import { LoginCard } from "./LoginCard";
import { LoginFooter } from "./LoginFooter";

import styles from "./LoginScreen.module.css";

/** The unauthenticated landing view: background photo, "Panel del tutor" hero, laptop+phone
 * showcase, white "Inicia sesión" card and footer.
 *
 * It renders only when there is no session: `app/page.tsx` already hands an authenticated tutor
 * straight to DashboardShell, so no redirect is needed here.
 *
 * D4: while AuthContext is still `loading`, the card shows the "Comprobando sesión…" spinner
 * instead of the Google button, so the button doesn't flash before the session check resolves.
 */
export function LoginScreen({ authStatus }: { authStatus: "loading" | "unauthenticated" }) {
  return (
    <main className={styles.screen}>
      <BackgroundScene />

      <div className={styles.hero}>
        <h2 className={styles.heroTitle}>Panel del tutor</h2>
        <p className={styles.heroSubtitle}>
          Administra dispositivos, aplicaciones y reglas desde un solo lugar.
        </p>
      </div>

      <HeroShowcase />

      <div className={styles.cardColumn}>
        <LoginCard authStatus={authStatus} />
      </div>

      <div className={styles.footerWrap}>
        <LoginFooter />
      </div>
    </main>
  );
}
