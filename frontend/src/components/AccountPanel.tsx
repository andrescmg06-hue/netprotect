"use client";

import { Globe, KeyRound, Languages, LogOut, type LucideIcon, MonitorSmartphone } from "lucide-react";
import { type ReactNode, useEffect, useId, useState } from "react";

import { Avatar } from "@/components/shell/Avatar";
import { Button, StatusBadge } from "@/components/ui";
import type { CurrentUser } from "@/lib/apiClient";

import styles from "./AccountPanel.module.css";

/** A read-only label/value pair of the settings list. Named `Fact`, not `Field`, so it no longer
 * collides with the form primitive `Field` exported by `@/components/ui`. */
function Fact({
  icon: Icon,
  label,
  value,
  detail,
  aside,
}: {
  icon: LucideIcon;
  label: string;
  value: ReactNode;
  detail?: string;
  aside?: ReactNode;
}) {
  return (
    <div className={styles.fact}>
      <dt className={styles.factLabel}>
        <Icon size={18} strokeWidth={1.75} aria-hidden="true" />
        {label}
      </dt>
      <dd className={styles.factValue}>
        <span className={styles.factText}>
          {value}
          {detail && <span className={styles.factDetail}>{detail}</span>}
        </span>
        {aside}
      </dd>
    </div>
  );
}

/** Sprint 24: the account card that used to sit directly in page.tsx became its own dashboard
 * section — no new data, same CurrentUser the AuthContext already holds after login.
 *
 * Sprint 33: timezone and language come from the browser (Intl/navigator), not the backend —
 * there is no endpoint for either. Read only after mount so server and client render the same
 * "—" placeholder first, avoiding a hydration mismatch. Account creation date and last-login
 * time are not shown: CurrentUser carries neither.
 *
 * Sprint 59: recomposed as a settings page — sections divided by lines, a title and a sentence on
 * the left, the facts on the right — instead of floating cards. The mockup's "Editar perfil",
 * photo upload, active-sessions list and "Cerrar todas las sesiones" are omitted: the API has no
 * profile editing and the session is only this browser's Google sign-in.
 */
export function AccountPanel({ user, onSignOut }: { user: CurrentUser; onSignOut: () => void }) {
  const [timezone, setTimezone] = useState<string | null>(null);
  const [language, setLanguage] = useState<string | null>(null);
  const accountTitleId = useId();
  const securityTitleId = useId();
  const preferencesTitleId = useId();
  const sessionTitleId = useId();

  // Sprint 19/24 pattern (see CLAUDE.md): react-hooks/set-state-in-effect rejects a synchronous
  // setState in the effect body, even for a pure browser read — chaining .then() on an already-
  // resolved promise keeps it inside a callback instead.
  useEffect(() => {
    Promise.resolve().then(() => {
      setTimezone(Intl.DateTimeFormat().resolvedOptions().timeZone);
      setLanguage(
        new Intl.DisplayNames(["es"], { type: "language" }).of(navigator.language.split("-")[0]) ?? navigator.language
      );
    });
  }, []);

  const displayName = user.display_name ?? user.email;

  return (
    <div className={styles.settings}>
      <section className={styles.section} aria-labelledby={accountTitleId}>
        <div className={styles.intro}>
          <h2 id={accountTitleId} className={styles.sectionTitle}>
            Cuenta
          </h2>
          <p className={styles.sectionText}>Tu nombre, tu correo y tu foto vienen de tu cuenta de Google.</p>
        </div>
        <div className={styles.identity}>
          <Avatar name={displayName} src={user.avatar_url} size={72} />
          <div className={styles.identityText}>
            <span className={styles.name}>{displayName}</span>
            <span className={styles.email}>{user.email}</span>
          </div>
          <StatusBadge tone="neutral">Cuenta de Google</StatusBadge>
        </div>
      </section>

      <section className={styles.section} aria-labelledby={securityTitleId}>
        <div className={styles.intro}>
          <h2 id={securityTitleId} className={styles.sectionTitle}>
            Seguridad
          </h2>
          <p className={styles.sectionText}>Entras con Google, sin una contraseña propia de NetProtect que proteger.</p>
        </div>
        <dl className={styles.facts}>
          <Fact
            icon={KeyRound}
            label="Acceso"
            value="Autenticación con Google"
            detail="Google OAuth 2.0"
            aside={<StatusBadge tone="success">Activa</StatusBadge>}
          />
          <Fact icon={MonitorSmartphone} label="Sesión" value="Sesión abierta solo en este navegador" />
        </dl>
      </section>

      <section className={styles.section} aria-labelledby={preferencesTitleId}>
        <div className={styles.intro}>
          <h2 id={preferencesTitleId} className={styles.sectionTitle}>
            Preferencias
          </h2>
          <p className={styles.sectionText}>Se toman de la configuración de este navegador.</p>
        </div>
        <dl className={styles.facts}>
          <Fact icon={Globe} label="Zona horaria" value={timezone ?? "—"} />
          <Fact icon={Languages} label="Idioma" value={language ?? "—"} />
        </dl>
      </section>

      <section className={styles.section} aria-labelledby={sessionTitleId}>
        <div className={styles.intro}>
          <h2 id={sessionTitleId} className={styles.sectionTitle}>
            Cerrar sesión
          </h2>
          <p className={styles.sectionText}>Se cerrará tu sesión en este navegador.</p>
        </div>
        <div className={styles.signOut}>
          <Button variant="danger" icon={LogOut} onClick={onSignOut} className={styles.signOutButton}>
            Cerrar sesión
          </Button>
        </div>
      </section>
    </div>
  );
}
