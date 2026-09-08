"use client";

import { useEffect, useState } from "react";

import { ApiError, type HistoryEvent, listDeviceHistory } from "@/lib/apiClient";

type HistoryState =
  | { kind: "loading" }
  | { kind: "loaded"; events: HistoryEvent[] }
  | { kind: "error"; message: string };

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

function eventLabel(event: HistoryEvent): string {
  if (event.event_type === "APP_RULE") {
    return `Bloqueo (${event.rule_type_applied}) de ${event.package_name}`;
  }
  return event.geofence_event_type === "ENTER"
    ? `Entró a ${event.geofence_name}`
    : `Salió de ${event.geofence_name}`;
}

/** Sprint 15: a single chronological timeline merging AppRuleEvent (bloqueos, Sprint 8) and
 * GeofenceEvent (entradas/salidas, Sprint 14) — the two event logs that already existed, read
 * through the new unified GET /devices/{id}/history endpoint rather than duplicated here.
 * Ubicación cruda no aparece: ya tiene su propia vista (DeviceLocationPanel) y mezclarla aquí
 * enterraría estos eventos discretos bajo hasta 96 puntos/día.
 */
export function HistoryPanel({ accessToken, deviceId }: { accessToken: string; deviceId: string }) {
  const [state, setState] = useState<HistoryState>({ kind: "loading" });
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    let cancelled = false;

    listDeviceHistory(accessToken, deviceId)
      .then(({ events }) => {
        if (!cancelled) {
          setState({ kind: "loaded", events });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setState({ kind: "error", message: describeError(error, "No se pudo cargar el historial") });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, reloadToken]);

  return (
    <div className="rulesPanel">
      <div className="devicesPanelHeader">
        <strong>Historial</strong>
        <button type="button" onClick={() => setReloadToken((current) => current + 1)}>
          Actualizar
        </button>
      </div>

      {state.kind === "loading" && <p className="statusText">Cargando historial…</p>}
      {state.kind === "error" && <p className="authError">{state.message}</p>}
      {state.kind === "loaded" && state.events.length === 0 && (
        <p className="statusText">Todavía no hay eventos registrados para este dispositivo.</p>
      )}
      {state.kind === "loaded" && state.events.length > 0 && (
        <ul className="appList">
          {state.events.map((event) => (
            <li key={event.id} className="appRow">
              <div className="appLabel">{eventLabel(event)}</div>
              <span className="appUsage">{new Date(event.occurred_at).toLocaleString("es-CO")}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
