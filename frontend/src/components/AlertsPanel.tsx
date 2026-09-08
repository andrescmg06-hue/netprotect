"use client";

import { useEffect, useState } from "react";

import {
  type Alert,
  type AlertSilence,
  ApiError,
  deleteAlertSilence,
  listAlertSilences,
  listDeviceAlerts,
  markAlertRead,
  silenceAlert,
} from "@/lib/apiClient";

type AlertsState =
  | { kind: "loading" }
  | { kind: "loaded"; alerts: Alert[]; silences: AlertSilence[] }
  | { kind: "error"; message: string };

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

function alertLabel(alert: Alert): string {
  switch (alert.alert_type) {
    case "APP_BLOCKED":
      return `Se bloqueó ${alert.package_name}`;
    case "APP_LIMIT_REACHED":
      return `Se alcanzó el límite de tiempo de ${alert.package_name}`;
    case "GEOFENCE_EXIT":
      return `Salió de ${alert.geofence_name}`;
    case "GEOFENCE_ENTER":
      return `Entró a ${alert.geofence_name}`;
    // Sprint 20 — señales de manipulación. Sin package_name ni geofence_name: describen el
    // estado del dispositivo, no una app ni una zona concreta.
    case "PERMISSION_REVOKED":
      return "El permiso de acceso a uso de apps no está activo: el dispositivo no puede aplicar reglas";
    case "SERVICE_INACTIVE":
      return "El servicio de control de apps no está en ejecución en el dispositivo";
    case "HEARTBEAT_SILENCE":
      return "El dispositivo dejó de reportarse durante un periodo anormalmente largo";
    case "CLOCK_TAMPERING":
      return "La hora del dispositivo no coincide con la del servidor";
    case "UNINSTALL_ATTEMPT":
      return "Se intentó desactivar la protección contra desinstalación";
  }
}

/** Sprint 17: a tutor inbox generated from signals that already existed (bloqueos de reglas,
 * entradas/salidas de geocercas) — read through GET /devices/{id}/alerts, which the backend
 * already deduplicates while unread. Marking read and silenciar son acciones de tutor (escriben
 * estado), mismo criterio que crear reglas/geocercas: sólo desde el panel web, no desde Android.
 */
export function AlertsPanel({ accessToken, deviceId }: { accessToken: string; deviceId: string }) {
  const [state, setState] = useState<AlertsState>({ kind: "loading" });
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    let cancelled = false;

    Promise.all([listDeviceAlerts(accessToken, deviceId), listAlertSilences(accessToken, deviceId)])
      .then(([alertsResponse, silencesResponse]) => {
        if (!cancelled) {
          setState({ kind: "loaded", alerts: alertsResponse.alerts, silences: silencesResponse.silences });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setState({ kind: "error", message: describeError(error, "No se pudieron cargar las alertas") });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, reloadToken]);

  function handleRead(alertId: string) {
    markAlertRead(accessToken, deviceId, alertId)
      .then(() => setReloadToken((current) => current + 1))
      .catch(() => setReloadToken((current) => current + 1));
  }

  function handleSilence(alertId: string) {
    silenceAlert(accessToken, deviceId, alertId, null)
      .then(() => setReloadToken((current) => current + 1))
      .catch(() => setReloadToken((current) => current + 1));
  }

  function handleUnsilence(silenceId: string) {
    deleteAlertSilence(accessToken, deviceId, silenceId)
      .then(() => setReloadToken((current) => current + 1))
      .catch(() => setReloadToken((current) => current + 1));
  }

  return (
    <div className="rulesPanel">
      <div className="devicesPanelHeader">
        <strong>Alertas</strong>
        <button type="button" onClick={() => setReloadToken((current) => current + 1)}>
          Actualizar
        </button>
      </div>

      {state.kind === "loading" && <p className="statusText">Cargando alertas…</p>}
      {state.kind === "error" && <p className="authError">{state.message}</p>}
      {state.kind === "loaded" && state.alerts.length === 0 && (
        <p className="statusText">Sin alertas para este dispositivo.</p>
      )}
      {state.kind === "loaded" && state.alerts.length > 0 && (
        <ul className="appList">
          {state.alerts.map((alert) => (
            <li key={alert.id} className="appRow">
              <div className="appLabel">
                [{alert.level}] {alertLabel(alert)}
                {alert.occurrence_count > 1 ? ` (x${alert.occurrence_count})` : ""}
                {alert.read_at ? " — leída" : ""}
              </div>
              <span className="appUsage">
                {new Date(alert.last_occurred_at).toLocaleString("es-CO")}
                {alert.read_at === null && (
                  <>
                    {" "}
                    <button type="button" onClick={() => handleRead(alert.id)}>
                      Marcar leída
                    </button>{" "}
                    <button type="button" onClick={() => handleSilence(alert.id)}>
                      Silenciar
                    </button>
                  </>
                )}
              </span>
            </li>
          ))}
        </ul>
      )}

      {state.kind === "loaded" && state.silences.length > 0 && (
        <>
          <p className="statusText">Silenciadas</p>
          <ul className="appList">
            {state.silences.map((silence) => (
              <li key={silence.id} className="appRow">
                <div className="appLabel">{silence.dedup_key}</div>
                <span className="appUsage">
                  {silence.silenced_until
                    ? `hasta ${new Date(silence.silenced_until).toLocaleString("es-CO")}`
                    : "indefinido"}{" "}
                  <button type="button" onClick={() => handleUnsilence(silence.id)}>
                    Reactivar
                  </button>
                </span>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}
