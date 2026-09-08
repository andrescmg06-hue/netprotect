"use client";

import { useEffect, useState } from "react";

import {
  ApiError,
  type DeviceStatisticsResponse,
  type StatisticsPeriod,
  getDeviceStatistics,
} from "@/lib/apiClient";

type StatisticsState =
  | { kind: "loading" }
  | { kind: "loaded"; data: DeviceStatisticsResponse }
  | { kind: "error"; message: string };

const PERIOD_LABELS: Record<StatisticsPeriod, string> = {
  today: "Hoy",
  "7d": "7 días",
  "30d": "30 días",
};

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

function formatDuration(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  if (hours > 0) return `${hours} h ${minutes} min`;
  if (minutes > 0) return `${minutes} min`;
  return "< 1 min";
}

function formatRate(rate: number | null): string {
  return rate === null ? "sin datos" : `${Math.round(rate * 100)}%`;
}

/** Sprint 16: on-the-fly aggregates over data that already existed — DeviceApplicationUsage
 * (apps más usadas / por categoría), AppRuleEvent (bloqueos) and AppRule/CategoryRule DAILY_LIMIT
 * rows (cumplimiento) — read through GET /devices/{id}/statistics. No chart library: this
 * project's frontend has none, and every other panel (HistoryPanel, GeofencePanel) is plain text
 * lists, so this one stays consistent rather than introducing a new dependency for one panel.
 */
export function StatisticsPanel({
  accessToken,
  deviceId,
}: {
  accessToken: string;
  deviceId: string;
}) {
  const [period, setPeriod] = useState<StatisticsPeriod>("today");
  const [state, setState] = useState<StatisticsState>({ kind: "loading" });
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    let cancelled = false;

    getDeviceStatistics(accessToken, deviceId, period)
      .then((data) => {
        if (!cancelled) {
          setState({ kind: "loaded", data });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setState({
            kind: "error",
            message: describeError(error, "No se pudieron cargar las estadísticas"),
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, period, reloadToken]);

  return (
    <div className="rulesPanel">
      <div className="devicesPanelHeader">
        <strong>Estadísticas</strong>
        <button type="button" onClick={() => setReloadToken((current) => current + 1)}>
          Actualizar
        </button>
      </div>

      <div className="devicesPanelHeader">
        {(Object.keys(PERIOD_LABELS) as StatisticsPeriod[]).map((candidate) => (
          <button
            key={candidate}
            type="button"
            disabled={candidate === period}
            onClick={() => setPeriod(candidate)}
          >
            {PERIOD_LABELS[candidate]}
          </button>
        ))}
      </div>

      {state.kind === "loading" && <p className="statusText">Cargando estadísticas…</p>}
      {state.kind === "error" && <p className="authError">{state.message}</p>}
      {state.kind === "loaded" && (
        <>
          <p className="statusText">
            Apps más usadas
          </p>
          {state.data.top_apps.length === 0 && (
            <p className="statusText">Sin datos de uso en este periodo.</p>
          )}
          <ul className="appList">
            {state.data.top_apps.map((entry) => (
              <li key={entry.package_name} className="appRow">
                <div className="appLabel">{entry.app_label ?? entry.package_name}</div>
                <span className="appUsage">{formatDuration(entry.total_seconds)}</span>
              </li>
            ))}
          </ul>

          <p className="statusText">Por categoría</p>
          {state.data.categories.length === 0 && (
            <p className="statusText">Sin datos de uso en este periodo.</p>
          )}
          <ul className="appList">
            {state.data.categories.map((entry) => (
              <li key={entry.category ?? "SIN_CATEGORIA"} className="appRow">
                <div className="appLabel">{entry.category ?? "Sin categoría"}</div>
                <span className="appUsage">{formatDuration(entry.total_seconds)}</span>
              </li>
            ))}
          </ul>

          <p className="statusText">Bloqueos</p>
          {state.data.blocks_by_reason.length === 0 && (
            <p className="statusText">Ningún bloqueo registrado en este periodo.</p>
          )}
          <ul className="appList">
            {state.data.blocks_by_reason.map((entry) => (
              <li key={entry.rule_type_applied} className="appRow">
                <div className="appLabel">{entry.rule_type_applied}</div>
                <span className="appUsage">{entry.count}</span>
              </li>
            ))}
          </ul>

          <p className="statusText">Cumplimiento de límites diarios</p>
          {state.data.compliance.length === 0 && (
            <p className="statusText">No hay reglas de límite diario configuradas.</p>
          )}
          <ul className="appList">
            {state.data.compliance.map((entry) => (
              <li
                key={`${entry.scope}-${entry.package_name ?? entry.category}`}
                className="appRow"
              >
                <div className="appLabel">
                  {entry.scope === "APP" ? entry.package_name : entry.category}
                  {" "}
                  (límite {entry.daily_limit_minutes} min/día)
                </div>
                <span className="appUsage">
                  {formatRate(entry.compliance_rate)} ({entry.days_compliant}/
                  {entry.days_evaluated} días)
                </span>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}
