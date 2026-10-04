import Image from "next/image";

import { Logo, Spinner } from "@/components/ui";

import { GoogleButton } from "./GoogleButton";

import styles from "./LoginScreen.module.css";

/** The unauthenticated landing view: a full-screen photograph with the sign-in panel over it as
 * frosted glass (translucent and blurred, so the photograph shows through). On narrow screens the
 * panel is a card at the bottom and the photograph fills the band above it. Only Google sign-in,
 * per the brief.
 *
 * It renders only when there is no session: `app/page.tsx` already hands an authenticated tutor
 * straight to DashboardShell, so no redirect is needed here.
 *
 * The logo is the official one (`ui/Logo`, public/brand/logo-full.png): the mockup draws its own
 * serif wordmark, but the official logo rules (docs/redesign/CONTINUAR.md §6) and the name is always
 * NetProtect. The serif is only the voice of the title, through the global h1 rule.
 *
 * D4: while AuthContext is still `loading`, the panel shows the "Comprobando sesión…" spinner
 * instead of the Google button, so the button doesn't flash before the session check resolves.
 */
export function LoginScreen({ authStatus }: { authStatus: "loading" | "unauthenticated" }) {
  return (
    <main className={styles.screen}>
      <section className={styles.panel}>
        <div className={styles.content}>
          <Logo height={56} />

          <span className={styles.rule} aria-hidden="true" />

          <h1 className={styles.title}>
            Inicia <br />
            sesión
          </h1>

          <p className={styles.subtitle}>
            Accede a tu panel para administrar dispositivos, aplicaciones y reglas.
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

          <div className={styles.divider} aria-hidden="true">
            <span className={styles.dot} />
          </div>

          <p className={styles.footnote}>Tu cuenta de Google es tu cuenta en NetProtect.</p>
        </div>
      </section>

      {/* Decorative: the photograph carries nothing the panel doesn't already state. It fills the
          whole screen behind the panel, so it loads eagerly instead of lazily. */}
      <div className={styles.photo} aria-hidden="true">
        <Image
          src="/login/study.jpg"
          alt=""
          fill
          loading="eager"
          fetchPriority="high"
          sizes="100vw"
          className={styles.image}
        />
      </div>
    </main>
  );
}
