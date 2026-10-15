"use client";

import { Ban, Clock, Link2, RefreshCw } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";

import { Button, Card } from "@/components/ui";
import { type PairingCode, generatePairingCode, revokePairingCode } from "@/lib/apiClient";
import { describeError } from "@/lib/errors";
import { formatCountdown } from "@/lib/format";

import styles from "./PairingPanel.module.css";

type PairingState =
  | { kind: "idle" }
  | { kind: "generating" }
  | { kind: "active"; pairing: PairingCode; secondsLeft: number; totalSeconds: number }
  | { kind: "expired" }
  | { kind: "revoked" }
  | { kind: "error"; message: string };

/** The countdown turns amber in its last half minute: time to hurry, not an error. */
const HURRY_SECONDS = 30;
const CODE_LENGTH = 6;
/** The step the tutor is on while a code is live: typing it into the app. */
const ENTER_CODE_STEP = 2;

// The app is installed by sideload, not from a store: no step names Google Play.
const STEPS = [
  { title: "Instala la app de NetProtect", detail: "En el dispositivo Android que quieres supervisar." },
  { title: "Abre la app y elige «Vincular dispositivo»", detail: "Desde la pantalla inicial de la app." },
  { title: "Ingresa el código de 6 dígitos", detail: "El que se genera aquí, antes de que expire." },
  { title: "Listo", detail: "El dispositivo aparecerá en Dispositivos." },
];

const STATUS_TEXT: Record<Exclude<PairingState["kind"], "error">, string> = {
  idle: "Todavía no hay un código activo.",
  generating: "Generando el código…",
  active: "Código activo. Ingrésalo en la app antes de que venza.",
  expired: "El código venció sin usarse. Genera uno nuevo cuando tengas el dispositivo a mano.",
  revoked: "Revocaste el código: ya no sirve para vincular.",
};

/** Sprint 24: the pairing flow (backend/app/api/v1/endpoints/pairing.py, Sprint 5) was reachable
 * only from Android until now — the web panel adds the same "generate/revoke the one live code"
 * calls, nothing new on the backend. The countdown is purely a client-side reflection of
 * expires_in_seconds; the backend remains the only source of truth for whether the code is still
 * redeemable (a stale tab showing "0:12" doesn't mean the code still works — codes are also
 * invalidated early by generating a new one, exactly as before).
 *
 * Sprint 55: a guided flow where the code is the protagonist (six large cells, split 3 + 3 to be
 * read aloud), with the real countdown from expires_in_seconds (the TTL is 180 s, never "10
 * minutes" as the mockup says) and an explicit "revoked" state. No image, no code history: the
 * backend keeps one live code per tutor and nothing else (docs/redesign/fase-0-informe.md §5). */
export function PairingPanel({ accessToken }: { accessToken: string }) {
  const [state, setState] = useState<PairingState>({ kind: "idle" });
  const intervalRef = useRef<number | null>(null);

  const clearTicker = useCallback(() => {
    if (intervalRef.current !== null) {
      window.clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  }, []);

  useEffect(() => clearTicker, [clearTicker]);

  const handleGenerate = useCallback(() => {
    clearTicker();
    setState({ kind: "generating" });
    generatePairingCode(accessToken)
      .then((pairing) => {
        setState({
          kind: "active",
          pairing,
          secondsLeft: pairing.expires_in_seconds,
          totalSeconds: pairing.expires_in_seconds,
        });
        intervalRef.current = window.setInterval(() => {
          setState((current) => {
            if (current.kind !== "active") {
              return current;
            }
            if (current.secondsLeft <= 1) {
              clearTicker();
              return { kind: "expired" };
            }
            return { ...current, secondsLeft: current.secondsLeft - 1 };
          });
        }, 1000);
      })
      .catch((error) => {
        setState({ kind: "error", message: describeError(error, "No se pudo generar el código") });
      });
  }, [accessToken, clearTicker]);

  const handleRevoke = useCallback(() => {
    clearTicker();
    revokePairingCode(accessToken)
      .then(() => setState({ kind: "revoked" }))
      .catch((error) => {
        setState({ kind: "error", message: describeError(error, "No se pudo revocar el código") });
      });
  }, [accessToken, clearTicker]);

  const active = state.kind === "active" ? state : null;
  const digits = active ? active.pairing.code.split("") : Array.from({ length: CODE_LENGTH }, () => "");
  const progress = active ? Math.max(0, active.secondsLeft / active.totalSeconds) : 0;
  const hurry = active !== null && active.secondsLeft <= HURRY_SECONDS;

  let generateLabel = "Generar un código nuevo";
  if (state.kind === "idle" || state.kind === "generating") generateLabel = "Generar código";
  if (state.kind === "error") generateLabel = "Reintentar";

  return (
    <div className={styles.layout}>
      <div className={styles.columns}>
        <Card className={styles.codePanel} aria-labelledby="pairing-code-title">
          <p className={`eyebrow ${styles.eyebrow}`}>Código de vinculación</p>
          <h2 id="pairing-code-title" className={styles.title}>
            Genera un código de vinculación
          </h2>
          <p className={styles.lead}>
            El código permite vincular la app de NetProtect en un dispositivo Android. Vale por tiempo limitado y
            solo puede usarse una vez.
          </p>

          <div className={styles.codeBlock}>
            <div className={active ? `${styles.code} ${styles.codeLive}` : styles.code} aria-hidden="true">
              {digits.map((digit, index) => (
                <span key={index} className={styles.cell}>
                  {digit || "–"}
                </span>
              ))}
            </div>
            {active && <p className={styles.srOnly}>Código: {digits.join(" ")}</p>}

            {active && (
              <div className={hurry ? `${styles.countdown} ${styles.hurry}` : styles.countdown}>
                <div className={styles.track}>
                  <div className={styles.fill} style={{ transform: `scaleX(${progress})` }} />
                </div>
                <p className={styles.timer}>
                  <Clock size={14} strokeWidth={2} aria-hidden="true" />
                  <span role="timer">Vence en {formatCountdown(active.secondsLeft)}</span>
                </p>
              </div>
            )}
          </div>

          {state.kind === "error" ? (
            <p className={styles.error} role="alert">
              {state.message}
            </p>
          ) : (
            <p className={styles.status} aria-live="polite">
              {STATUS_TEXT[state.kind]}
            </p>
          )}

          <div className={styles.actions}>
            {active ? (
              <>
                <Button variant="ghost" icon={RefreshCw} onClick={handleGenerate}>
                  Generar otro código
                </Button>
                <Button variant="danger" icon={Ban} onClick={handleRevoke}>
                  Revocar código
                </Button>
              </>
            ) : (
              <Button
                variant="primary"
                icon={Link2}
                loading={state.kind === "generating"}
                onClick={handleGenerate}
              >
                {generateLabel}
              </Button>
            )}
          </div>
        </Card>

        <section className={styles.guide} aria-labelledby="pairing-steps-title">
          <p className={`eyebrow ${styles.eyebrow}`}>Paso a paso</p>
          <h2 id="pairing-steps-title" className={styles.title}>
            ¿Cómo vincular un dispositivo?
          </h2>
          <p className={styles.lead}>Sigue estos pasos en el dispositivo Android que quieres supervisar.</p>
          <ol className={styles.steps}>
            {STEPS.map((step, index) => {
              const current = active !== null && index === ENTER_CODE_STEP;
              return (
                <li
                  key={step.title}
                  className={current ? `${styles.step} ${styles.stepCurrent}` : styles.step}
                  aria-current={current ? "step" : undefined}
                >
                  <span className={styles.stepNumber} aria-hidden="true">
                    {index + 1}
                  </span>
                  <div className={styles.stepText}>
                    <p className={styles.stepTitle}>{step.title}</p>
                    <p className={styles.stepDetail}>{step.detail}</p>
                  </div>
                </li>
              );
            })}
          </ol>
        </section>
      </div>
    </div>
  );
}
