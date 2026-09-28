"use client";

import { Globe, KeyRound, Languages, LogOut, Mail, ShieldCheck, User as UserIcon } from "lucide-react";
import { useEffect, useState } from "react";

import { Avatar } from "@/components/shell/Avatar";
import { Button, Card, CardHeader, StatusBadge } from "@/components/ui";
import type { CurrentUser } from "@/lib/apiClient";

import styles from "./AccountPanel.module.css";

function Field({ icon: Icon, label, value }: { icon: typeof UserIcon; label: string; value: string }) {
  return (
    <div className={styles.field}>
      <Icon size={18} strokeWidth={1.8} className={styles.fieldIcon} aria-hidden="true" />
      <span className={styles.fieldLabel}>{label}</span>
      <span className={styles.fieldValue}>{value}</span>
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
 */
export function AccountPanel({ user, onSignOut }: { user: CurrentUser; onSignOut: () => void }) {
  const [timezone, setTimezone] = useState<string | null>(null);
  const [language, setLanguage] = useState<string | null>(null);

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
    <>
      <div className={styles.columns}>
        <Card>
          <CardHeader icon={UserIcon} title="Información de la cuenta" />

          <div className={styles.profile}>
            <Avatar name={displayName} src={user.avatar_url} size={64} />
            <div className={styles.profileText}>
              <span className={styles.profileName}>{displayName}</span>
              <span className={styles.profileEmail}>{user.email}</span>
            </div>
            <StatusBadge tone="info">Cuenta de Google</StatusBadge>
          </div>

          <div className={styles.fields}>
            <Field icon={UserIcon} label="Nombre completo" value={displayName} />
            <Field icon={Mail} label="Correo electrónico" value={user.email} />
            <Field icon={KeyRound} label="Tipo de cuenta" value="Cuenta de Google" />
            <Field icon={Globe} label="Zona horaria" value={timezone ?? "—"} />
            <Field icon={Languages} label="Idioma" value={language ?? "—"} />
          </div>
        </Card>

        <Card>
          <CardHeader icon={ShieldCheck} title="Seguridad de la cuenta" subtitle="Tu cuenta está protegida con Google OAuth." />

          <div className={styles.securityRow}>
            <span className={`${styles.securityIcon} ${styles.securityOk}`}>
              <ShieldCheck size={20} aria-hidden="true" />
            </span>
            <div className={styles.securityText}>
              <strong>Cuenta segura</strong>
              <span>Estás autenticado mediante Google, sin contraseña propia que proteger.</span>
            </div>
          </div>

          <div className={styles.securityRow}>
            <span className={styles.securityIcon}>
              <KeyRound size={20} aria-hidden="true" />
            </span>
            <div className={styles.securityText}>
              <strong>Autenticación</strong>
              <span>Google OAuth 2.0</span>
            </div>
            <StatusBadge tone="success">Activa</StatusBadge>
          </div>

          <div className={styles.signOutBlock}>
            <Button variant="danger" icon={LogOut} fullWidth onClick={onSignOut}>
              Cerrar sesión
            </Button>
            <p className={styles.signOutHint}>Se cerrará tu sesión en este navegador.</p>
          </div>
        </Card>
      </div>
    </>
  );
}
