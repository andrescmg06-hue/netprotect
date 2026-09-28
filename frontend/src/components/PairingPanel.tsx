"use client";

import { Ban, Link2, RefreshCw, Smartphone } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";

import { Button, Card, CardHeader } from "@/components/ui";
import { ApiError, type PairingCode, generatePairingCode, revokePairingCode } from "@/lib/apiClient";

import styles from "./PairingPanel.module.css";

type PairingState =
  | { kind: "idle" }
  | { kind: "generating" }
  | { kind: "active"; pairing: PairingCode; secondsLeft: number; totalSeconds: number }
  | { kind: "expired" }
  | { kind: "error"; message: string };

const STEPS = [
  { title: "Instala la app de NetProtect", detail: "En el dispositivo Android que quieres supervisar." },
  { title: "Abre la app y elige “Vincular dispositivo”", detail: "Desde la pantalla inicial de la app." },
  { title: "Ingresa el código de 6 dígitos", detail: "El que se genera aquí, antes de que expire." },
  { title: "Listo", detail: "El dispositivo aparecerá en tu lista de vinculados." },
];

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

function formatSeconds(total: number): string {
  const minutes = Math.floor(total / 60);
  const seconds = total % 60;
  return `${minutes}:${seconds.toString().padStart(2, "0")}`;
}

/** Sprint 24: the pairing flow (backend/app/api/v1/endpoints/pairing.py, Sprint 5) was reachable
 * only from Android until now — the web panel adds the same "generate/revoke the one live code"
 * calls, nothing new on the backend. The countdown is purely a client-side reflection of
 * expires_in_seconds; the backend remains the only source of truth for whether the code is still
 * redeemable (a stale tab showing "00:12" doesn't mean the code still works — codes are also
 * invalidated early by generating a new one, exactly as before).
 */
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
      .then(() => setState({ kind: "idle" }))
      .catch((error) => {
        setState({ kind: "error", message: describeError(error, "No se pudo revocar el código") });
      });
  }, [accessToken, clearTicker]);

  const progress = state.kind === "active" ? Math.max(0, state.secondsLeft / state.totalSeconds) : 0;

  return (
    <div className={styles.columns}>
      <Card>
        <CardHeader
          icon={Link2}
          title="Generar código de vinculación"
          subtitle="El código permite vincular la app de NetProtect en un dispositivo. Es válido por un tiempo limitado y solo puede usarse una vez."
        />

        {(state.kind === "idle" || state.kind === "expired") && (
          <div className={styles.idle}>
            {state.kind === "expired" && <p className={styles.error}>El código expiró sin usarse.</p>}
            <Button variant="primary" icon={Link2} onClick={handleGenerate}>
              Generar código
            </Button>
          </div>
        )}

        {state.kind === "generating" && <p className={styles.hint}>Generando código…</p>}

        {state.kind === "error" && (
          <div className={styles.idle}>
            <p className={styles.error}>{state.message}</p>
            <Button variant="primary" onClick={handleGenerate}>
              Reintentar
            </Button>
          </div>
        )}

        {state.kind === "active" && (
          <div className={styles.active}>
            <span className={styles.digits}>{state.pairing.code}</span>
            <div className={styles.progressTrack}>
              <div className={styles.progressFill} style={{ transform: `scaleX(${progress})` }} />
            </div>
            <span className={styles.hint}>Válido por {formatSeconds(state.secondsLeft)} minutos</span>
            <div className={styles.actions}>
              <Button icon={RefreshCw} onClick={handleGenerate}>
                Generar otro código
              </Button>
              <Button variant="danger" icon={Ban} onClick={handleRevoke}>
                Revocar código
              </Button>
            </div>
          </div>
        )}
      </Card>

      <Card>
        <CardHeader icon={Smartphone} title="¿Cómo vincular un dispositivo?" />
        <ol className={styles.steps}>
          {STEPS.map((step, index) => (
            <li key={step.title} className={styles.step}>
              <span className={styles.stepNumber}>{index + 1}</span>
              <div>
                <strong>{step.title}</strong>
                <p>{step.detail}</p>
              </div>
            </li>
          ))}
        </ol>
      </Card>
    </div>
  );
}
