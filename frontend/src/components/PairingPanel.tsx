"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import { ApiError, type PairingCode, generatePairingCode, revokePairingCode } from "@/lib/apiClient";

type PairingState =
  | { kind: "idle" }
  | { kind: "generating" }
  | { kind: "active"; pairing: PairingCode; secondsLeft: number }
  | { kind: "expired" }
  | { kind: "error"; message: string };

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
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
        setState({ kind: "active", pairing, secondsLeft: pairing.expires_in_seconds });
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

  function formatSeconds(total: number): string {
    const minutes = Math.floor(total / 60);
    const seconds = total % 60;
    return `${minutes}:${seconds.toString().padStart(2, "0")}`;
  }

  return (
    <div className="devicesPanel">
      <div className="devicesPanelHeader">
        <strong>Vinculación de dispositivos</strong>
      </div>
      <p className="statusText" style={{ textAlign: "left" }}>
        Genera un código de 6 dígitos e ingrésalo en la app NetProtect del dispositivo que quieres
        supervisar. El código es válido una sola vez y expira a los 3 minutos.
      </p>

      {(state.kind === "idle" || state.kind === "expired") && (
        <>
          {state.kind === "expired" && <p className="authError">El código expiró sin usarse.</p>}
          <button type="button" onClick={handleGenerate}>
            Generar código
          </button>
        </>
      )}

      {state.kind === "generating" && <p className="statusText">Generando código…</p>}

      {state.kind === "error" && (
        <>
          <p className="authError">{state.message}</p>
          <button type="button" onClick={handleGenerate}>
            Reintentar
          </button>
        </>
      )}

      {state.kind === "active" && (
        <div className="pairingCode">
          <span className="pairingCodeDigits">{state.pairing.code}</span>
          <span className="statusText">Expira en {formatSeconds(state.secondsLeft)}</span>
          <div className="deviceActions">
            <button type="button" onClick={handleGenerate}>
              Generar otro
            </button>
            <button type="button" className="dangerButton" onClick={handleRevoke}>
              Revocar
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
